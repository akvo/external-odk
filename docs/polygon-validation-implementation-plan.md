# Polygon Overlap Validation - Implementation Plan

## Existing Resources

| Component | Location | Purpose |
|-----------|----------|---------|
| `SubmissionEntity` | `data/entity/` | Stores submissions with `rawData` JSON + `instanceName` |
| `SubmissionDao` | `data/dao/` | Queries by assetUid, uuid |
| `PolygonValidator` | `validation/` | JTS validation (vertices, area, self-intersection) |
| `PolygonValidationActivity` | `validation/` | Intent receiver, returns "value" to Kobo |
| `KoboRepository` | `data/repository/` | Fetches/syncs submissions from Kobo API |

**Already integrated**: JTS library, Room, Hilt, Kobo API sync

---

## Entity Relationship Diagram

```mermaid
erDiagram
    submissions {
        string _uuid PK
        string assetUid FK
        string _id
        long submissionTime
        string submittedBy
        string instanceName "enumerator-woreda-date"
        string rawData "JSON with polygon + farmer names"
        string systemData
    }

    plots {
        string uuid PK
        string plotName "Farmer full name for display"
        string instanceName "For draft-to-submission matching"
        string polygonWkt "WKT format"
        double minLat "bbox"
        double maxLat "bbox"
        double minLon "bbox"
        double maxLon "bbox"
        boolean isDraft "true until synced"
        string formId FK
        string region "regional filter"
        string subRegion "regional filter"
        long createdAt
        string submissionUuid FK "null for drafts"
    }

    form_metadata {
        string assetUid PK
        long lastSyncTimestamp
    }

    submissions ||--o| plots : "links via instanceName"
    form_metadata ||--o{ submissions : "tracks sync"
```

---

## Validation Flow

```mermaid
flowchart TD
    A[Kobo Form] -->|Intent: shape, plot_name, region, sub_region| B[PolygonValidationActivity]
    B --> C[Parse Polygon]
    C --> D{Valid Format?}
    D -->|No| E[Return Error]
    D -->|Yes| F[Single Polygon Checks]
    F --> G{Pass vertices, area, self-intersect?}
    G -->|No| H[Return Error]
    G -->|Yes| I[Compute Bounding Box]
    I --> J[Query: Nearby Plots by region + bbox]
    J --> K[JTS intersects check]
    K --> L{Any Overlap?}
    L -->|Yes| M[Show Map + Return Error]
    L -->|No| N[Save as Draft Plot]
    N --> O[Return Success]

    M --> P[Map View with Overlaps]
    P --> Q[User sees conflict]
    Q --> R[Return to Kobo to fix]
```

---

## Proximity Filtering Strategy

```mermaid
flowchart LR
    subgraph "Step 1: Regional Filter"
        A[New Plot] --> B{Same region?}
        B -->|Yes| C[Include]
        B -->|No| D[Exclude - too far]
    end

    subgraph "Step 2: Bounding Box Filter"
        C --> E{bbox intersects?}
        E -->|Yes| F[Candidate]
        E -->|No| G[Exclude]
    end

    subgraph "Step 3: JTS Precise Check"
        F --> H[JTS intersects]
        H --> I{Overlaps?}
        I -->|Yes| J[Add to overlap list]
        I -->|No| K[Skip]
    end
```

**Performance**: Plots in different regions are never checked. Bounding box reduces JTS checks to ~5-50 candidates even with 10,000 plots.

---

## Draft Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Draft: Validation launch saves plot
    Draft --> Synced: Sync finds matching instanceName
    Draft --> Draft: Re-validation updates plot
    Synced --> Synced: Normal state

    note right of Draft
        isDraft = true
        submissionUuid = null
    end note

    note right of Synced
        isDraft = false
        submissionUuid = submission._uuid
    end note
```

**Match criteria**: `plots.instanceName` matches `submissions.instanceName`

---

## Intent Contract

### Current
```
appearance: ex:org.akvo.afribamodkvalidator.VALIDATE_POLYGON(shape=${Open_Area_GeoMapping})
```

### Required
```
appearance: ex:org.akvo.afribamodkvalidator.VALIDATE_POLYGON(
  shape=${Open_Area_GeoMapping},
  plot_name=${First_Name} ${Father_s_Name} ${Grandfather_s_Name},
  region=${woreda},
  sub_region=${kebele}
)
```

### XLSForm Settings (unchanged)
```
instance_name: concat(${enumerator_id}, '-', ${woreda}, '-', today())
```

```mermaid
sequenceDiagram
    participant K as Kobo Form
    participant V as ValidationActivity
    participant DB as Room Database
    participant M as Map View

    K->>V: Intent (shape, plot_name, region, sub_region)
    V->>V: Parse & validate polygon
    V->>DB: Query nearby plots (region + bbox)
    DB-->>V: Candidate plots
    V->>V: JTS intersects check

    alt No Overlap
        V->>DB: Save draft plot (plotName, instanceName from meta)
        V->>K: RESULT_OK (value=shape)
    else Overlap Found
        V->>M: Show map with overlapping plots
        M-->>V: User reviews conflicts
        V->>K: RESULT_OK (value=null, error_message)
    end
```

---

## Map Visualization

```mermaid
flowchart TB
    subgraph MapScreen
        A[OSMDroid Map View]
        B[Current Plot - Blue polygon]
        C[Overlapping Plots - Red polygons]
        D[Toast notification on tap]
    end

    A --> B
    A --> C
    C -->|tap| D
    D --> E[Shows: plotName]

    subgraph Implementation
        F[Uses MAPNIK tile source]
        G[Zoom 18 default]
        H[Auto-centers on polygons]
    end
```

---

## XLSForm Field Mapping

| XLSForm Field | Intent Extra | PlotEntity Field |
|---------------|--------------|------------------|
| `${First_Name} ${Father_s_Name} ${Grandfather_s_Name}` | `plot_name` | `plotName` |
| `meta/instanceName` | (from submission) | `instanceName` |
| `Open_Area_GeoMapping` or `manual_boundary` | `shape` | `polygonWkt` |
| `woreda` | `region` | `region` |
| `kebele` | `sub_region` | `subRegion` |

**rawData fields for sync extraction**:
- Polygon: `Open_Area_GeoMapping` (geotrace) or `manual_boundary` (geoshape)
- Farmer name: `First_Name`, `Father_s_Name`, `Grandfather_s_Name`

---

## Implementation Phases

### Phase 1: Data Layer
- [ ] Create `PlotEntity` with plotName, instanceName, bbox, regional columns
- [ ] Create `PlotDao` with proximity query
- [ ] Update `AppDatabase` to version 3
- [ ] Add `PlotDao` to `DatabaseModule`

### Phase 2: Overlap Detection
- [ ] Add `OverlapChecker` class using JTS
- [ ] Implement bbox computation from polygon
- [ ] Proximity filter: region + bbox pre-filter
- [ ] JTS `intersects()` for precise check

### Phase 3: Activity Integration
- [ ] Update `PolygonValidationActivity` for Hilt injection
- [ ] Parse intent extras (shape, plot_name, region, sub_region)
- [ ] Save draft plot on validation launch
- [ ] Format error message with plotName

### Phase 4: Map Visualization (MVP Required)
- [x] Integrate OSMDroid (replaced Mapbox for simplicity)
- [x] Display current polygon (blue) + overlapping polygons (red)
- [x] Toast notification with plotName on tap
- [ ] Offline tile caching (future enhancement)

### Phase 5: Draft Sync
- [ ] Match drafts to submissions by instanceName after sync
- [ ] Update `isDraft` = false on match
- [ ] Extract plots from synced rawData (polygon + farmer name fields)

### Phase 6: XLSForm Update
- [ ] Update intent appearance to pass plot_name, region, sub_region
- [ ] Test with updated form

---

## Error Message Format

Per AC:
```
New plot for <plotName> overlaps with plot for <plotName>
```

Example:
```
New plot for Abebe Kebede Tadesse overlaps with plot for Girma Tesfaye Hailu
```

---

## SQL Queries

### Proximity + Bounding Box Query
```sql
SELECT * FROM plots
WHERE uuid != :excludeUuid
  AND region = :region
  AND minLat <= :newMaxLat
  AND maxLat >= :newMinLat
  AND minLon <= :newMaxLon
  AND maxLon >= :newMinLon
```

### Match Draft to Submission
```sql
UPDATE plots
SET isDraft = 0, submissionUuid = :uuid
WHERE instanceName = :instanceName
  AND isDraft = 1
```

---

## Dependencies

**Already have:**
- `org.locationtech.jts:jts-core`

**Need to add:**
- `org.osmdroid:osmdroid-android:6.1.18` (for map visualization) ✅ Added

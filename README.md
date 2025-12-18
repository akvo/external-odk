# ODK Collect External Geoshape Validator

This Android app provides an external validation service for ODK Collect. It checks if every vertex of a captured `geoshape` lies inside a predefined polygon defined in `app/src/main/assets/polygon.json`.

## XLSForm Integration

To use this in your ODK form, use the `ex:` appearance prefix in a `geoshape` or `text` field.

| type | name | label | appearance |
| :--- | :--- | :--- | :--- |
| geoshape | field_name | Capture Shape | ex:com.akvo.externalodk.VALIDATE_GEOSHAPE(shape=.) |

### How it works
1. ODK Collect launches this app and passes the current shape via the `shape` extra.
2. This app parses the vertices and checks them against the internal GeoJSON.
3. It returns a result in a `value` extra:
   - `"valid"`: If all points are inside.
   - `"invalid: vertex N outside polygon"`: If validation fails.
   - `"invalid input: ..."`: If there is a parsing or resource error.

## Technical Details

### Intent Filter
The Activity is registered to handle the following action:
`com.akvo.externalodk.VALIDATE_GEOSHAPE`

### Data Formats
- **Input (`shape` extra):** Semicolon-separated coordinates (ODK default).
  - Example: `-7.3912 109.4652 0 0; -7.3913 109.4653 0 0`
- **Output (`value` extra):** String result used for validation logic in ODK.

### Local Testing (ADB)
You can simulate an ODK call using the following command:
```bash
adb shell am start -n com.akvo.externalodk/.GeoshapeValidationActivity \
  --es "shape" "-7.39123 109.46522 101.0 5.8"
```

## Setup & Customization
1. **Change the Polygon:** Update `app/src/main/assets/polygon.json` with your specific GeoJSON FeatureCollection (containing one Polygon).
2. **Build & Install:** Deploy the APK to the same device running ODK Collect.

For more information on external apps, see the [ODK Documentation](https://docs.getodk.org/collect-external-apps/).

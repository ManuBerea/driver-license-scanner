# OCR Engine Comparison Report

Generated: 2026-01-28T10:00:36.282101500Z

Total samples: 30

## Engine: paddle

### Bucket: clean

| Metric | Value |
| --- | --- |
| Required-field accuracy | 73.3% (FAIL) |
| Median scan-form time | 2369 ms (PASS) |
| P95 scan-form time | 10204 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 80.0% |
| lastName | 70.0% |
| dateOfBirth | 100.0% |
| addressLine | 50.0% |
| licenceNumber | 80.0% |
| expiryDate | 60.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_expiryDate | 4 |
| invalid_addressLine | 4 |
| invalid_lastName | 3 |
| missing_licenceNumber | 1 |
| missing_firstName | 1 |

### Bucket: medium

| Metric | Value |
| --- | --- |
| Required-field accuracy | 90.0% (PASS) |
| Median scan-form time | 2092 ms (PASS) |
| P95 scan-form time | 2207 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 100.0% |
| lastName | 100.0% |
| dateOfBirth | 100.0% |
| addressLine | 40.0% |
| licenceNumber | 100.0% |
| expiryDate | 100.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| invalid_addressLine | 6 |

### Bucket: poor

| Metric | Value |
| --- | --- |
| Required-field accuracy | 0.0% (FAIL) |
| Median scan-form time | 2069 ms (PASS) |
| P95 scan-form time | 2865 ms |
| % scans < 0.70 confidence | 100.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 0.0% |
| lastName | 0.0% |
| dateOfBirth | 0.0% |
| addressLine | 0.0% |
| licenceNumber | 0.0% |
| expiryDate | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_firstName | 10 |
| missing_lastName | 10 |
| missing_dateOfBirth | 10 |
| missing_addressLine | 10 |
| missing_licenceNumber | 10 |

### Bucket: all

| Metric | Value |
| --- | --- |
| Required-field accuracy | 54.4% (FAIL) |
| Median scan-form time | 2097 ms (PASS) |
| P95 scan-form time | 3194 ms |
| % scans < 0.70 confidence | 33.3% |
| Samples | 30 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 60.0% |
| lastName | 56.7% |
| dateOfBirth | 66.7% |
| addressLine | 30.0% |
| licenceNumber | 60.0% |
| expiryDate | 53.3% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_expiryDate | 14 |
| missing_licenceNumber | 11 |
| missing_firstName | 11 |
| missing_addressLine | 11 |
| invalid_addressLine | 10 |

## Engine: vision

### Bucket: clean

| Metric | Value |
| --- | --- |
| Required-field accuracy | 86.7% (PASS) |
| Median scan-form time | 439 ms (PASS) |
| P95 scan-form time | 924 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 100.0% |
| lastName | 100.0% |
| dateOfBirth | 100.0% |
| addressLine | 50.0% |
| licenceNumber | 70.0% |
| expiryDate | 100.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| invalid_addressLine | 5 |
| invalid_licenceNumber | 3 |

### Bucket: medium

| Metric | Value |
| --- | --- |
| Required-field accuracy | 83.3% (FAIL) |
| Median scan-form time | 491 ms (PASS) |
| P95 scan-form time | 550 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 100.0% |
| lastName | 100.0% |
| dateOfBirth | 100.0% |
| addressLine | 30.0% |
| licenceNumber | 70.0% |
| expiryDate | 100.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| invalid_addressLine | 7 |
| invalid_licenceNumber | 3 |

### Bucket: poor

| Metric | Value |
| --- | --- |
| Required-field accuracy | 35.0% (FAIL) |
| Median scan-form time | 535 ms (PASS) |
| P95 scan-form time | 612 ms |
| % scans < 0.70 confidence | 30.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 50.0% |
| lastName | 20.0% |
| dateOfBirth | 80.0% |
| addressLine | 10.0% |
| licenceNumber | 30.0% |
| expiryDate | 20.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_expiryDate | 8 |
| invalid_lastName | 7 |
| invalid_licenceNumber | 7 |
| invalid_addressLine | 6 |
| missing_firstName | 5 |

### Bucket: all

| Metric | Value |
| --- | --- |
| Required-field accuracy | 68.3% (FAIL) |
| Median scan-form time | 484 ms (PASS) |
| P95 scan-form time | 612 ms |
| % scans < 0.70 confidence | 10.0% |
| Samples | 30 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 83.3% |
| lastName | 73.3% |
| dateOfBirth | 93.3% |
| addressLine | 30.0% |
| licenceNumber | 56.7% |
| expiryDate | 73.3% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| invalid_addressLine | 18 |
| invalid_licenceNumber | 13 |
| missing_expiryDate | 8 |
| invalid_lastName | 7 |
| missing_firstName | 5 |


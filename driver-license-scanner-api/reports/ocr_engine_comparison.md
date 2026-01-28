# OCR Engine Comparison Report

Generated: 2026-01-28T08:53:44.796261Z

Total samples: 30

## Engine: paddle

### Bucket: clean

| Metric | Value |
| --- | --- |
| Required-field accuracy | 10.0% (FAIL) |
| Median scan-form time | 2225 ms (PASS) |
| P95 scan-form time | 15484 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 80.0% |
| lastName | 70.0% |
| dateOfBirth | 100.0% |
| addressLine | 30.0% |
| licenceNumber | 80.0% |
| expiryDate | 60.0% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 10 |
| mismatch_addressLine | 6 |
| missing_expiryDate | 4 |
| mismatch_lastName | 3 |
| missing_licenceNumber | 1 |

### Bucket: medium

| Metric | Value |
| --- | --- |
| Required-field accuracy | 20.0% (FAIL) |
| Median scan-form time | 2167 ms (PASS) |
| P95 scan-form time | 2528 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 100.0% |
| lastName | 100.0% |
| dateOfBirth | 100.0% |
| addressLine | 20.0% |
| licenceNumber | 100.0% |
| expiryDate | 100.0% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 10 |
| mismatch_addressLine | 8 |

### Bucket: poor

| Metric | Value |
| --- | --- |
| Required-field accuracy | 0.0% (FAIL) |
| Median scan-form time | 1846 ms (PASS) |
| P95 scan-form time | 2010 ms |
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
| categories | 0.0% |

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
| Required-field accuracy | 10.0% (FAIL) |
| Median scan-form time | 2117 ms (PASS) |
| P95 scan-form time | 5398 ms |
| % scans < 0.70 confidence | 33.3% |
| Samples | 30 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 60.0% |
| lastName | 56.7% |
| dateOfBirth | 66.7% |
| addressLine | 16.7% |
| licenceNumber | 60.0% |
| expiryDate | 53.3% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 30 |
| mismatch_addressLine | 14 |
| missing_expiryDate | 14 |
| missing_licenceNumber | 11 |
| missing_firstName | 11 |

## Engine: vision

### Bucket: clean

| Metric | Value |
| --- | --- |
| Required-field accuracy | 40.0% (FAIL) |
| Median scan-form time | 456 ms (PASS) |
| P95 scan-form time | 808 ms |
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
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 10 |
| mismatch_addressLine | 5 |
| mismatch_licenceNumber | 3 |

### Bucket: medium

| Metric | Value |
| --- | --- |
| Required-field accuracy | 10.0% (FAIL) |
| Median scan-form time | 521 ms (PASS) |
| P95 scan-form time | 603 ms |
| % scans < 0.70 confidence | 0.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 100.0% |
| lastName | 100.0% |
| dateOfBirth | 100.0% |
| addressLine | 20.0% |
| licenceNumber | 70.0% |
| expiryDate | 100.0% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 10 |
| mismatch_addressLine | 8 |
| mismatch_licenceNumber | 3 |

### Bucket: poor

| Metric | Value |
| --- | --- |
| Required-field accuracy | 10.0% (FAIL) |
| Median scan-form time | 497 ms (PASS) |
| P95 scan-form time | 579 ms |
| % scans < 0.70 confidence | 20.0% |
| Samples | 10 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 80.0% |
| lastName | 80.0% |
| dateOfBirth | 90.0% |
| addressLine | 10.0% |
| licenceNumber | 30.0% |
| expiryDate | 50.0% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 10 |
| mismatch_addressLine | 7 |
| mismatch_licenceNumber | 7 |
| missing_expiryDate | 5 |
| missing_firstName | 2 |

### Bucket: all

| Metric | Value |
| --- | --- |
| Required-field accuracy | 20.0% (FAIL) |
| Median scan-form time | 499 ms (PASS) |
| P95 scan-form time | 603 ms |
| % scans < 0.70 confidence | 6.7% |
| Samples | 30 |
| OCR failures | 0 |

| Field | Accuracy |
| --- | --- |
| firstName | 93.3% |
| lastName | 93.3% |
| dateOfBirth | 96.7% |
| addressLine | 26.7% |
| licenceNumber | 56.7% |
| expiryDate | 83.3% |
| categories | 0.0% |

**Top failure patterns**

| Pattern | Count |
| --- | --- |
| missing_categories | 30 |
| mismatch_addressLine | 20 |
| mismatch_licenceNumber | 13 |
| missing_expiryDate | 5 |
| missing_firstName | 2 |


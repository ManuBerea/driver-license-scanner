package com.dls.driverlicensescannerapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "OCR_WORKER_URL=http://localhost:8000",
        "X_INTERNAL_KEY=test-internal-key"
})
class DriverLicenseScannerApiApplicationTests {

    @Test
    void contextLoads() {
    }

}

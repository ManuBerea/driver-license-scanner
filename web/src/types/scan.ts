export type ScanValidation = {
  blockingErrors?: Array<{ code: string; field?: string | null; message?: string }>;
  warnings?: string[];
};

export type ScanFields = {
  firstName?: string | null;
  lastName?: string | null;
  dateOfBirth?: string | null;
  addressLine?: string | null;
  licenceNumber?: string | null;
  expiryDate?: string | null;
  categories?: string[];
};

export type ScanResult = {
  requestId: string;
  selectedEngine?: string;
  attemptedEngines?: string[];
  ocrConfidence?: number;
  confidenceThreshold?: number;
  processingTimeMs?: number;
  fields?: ScanFields;
  validation?: ScanValidation;
};

export type ScanErrorPayload = {
  error?: {
    code?: string;
    message?: string;
  };
};

FROM python:3.10-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY requirements-doctr.txt ./

ARG ENABLE_DOCTR=false
RUN if [ "$ENABLE_DOCTR" = "true" ]; then pip install --no-cache-dir -r requirements-doctr.txt; fi

COPY main.py ./
COPY ocr_engines.py ./

EXPOSE 8000

CMD ["uvicorn", "main:application", "--host", "0.0.0.0", "--port", "8000"]

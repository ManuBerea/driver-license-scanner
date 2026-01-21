from fastapi import FastAPI

def create_app() -> FastAPI:
    app = FastAPI(title="Driver License Scanner", version="0.1.0")

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    return app

application = create_app()
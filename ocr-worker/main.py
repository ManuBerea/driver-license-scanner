from fastapi import FastAPI

def create_app() -> FastAPI:
    app = FastAPI(title="Driver License Scanner", version="0.1.0")
    return app

application = create_app()
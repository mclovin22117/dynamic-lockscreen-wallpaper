import base64
import os
from pathlib import Path
from typing import Optional

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from dotenv import load_dotenv

ENV_PATH = Path(__file__).resolve().parent / ".env"
load_dotenv(dotenv_path=ENV_PATH)

app = FastAPI(title="Dynamic Lock Spotify Cover API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_token_cache: dict[str, str] = {}


class CoverResponse(BaseModel):
    trackId: str
    imageUrl: str
    width: Optional[int] = None
    height: Optional[int] = None
    albumName: Optional[str] = None
    artistName: Optional[str] = None


async def verify_api_token(x_api_token: Optional[str] = Header(default=None)) -> None:
    backend_api_token = os.getenv("BACKEND_API_TOKEN", "")
    if backend_api_token and x_api_token != backend_api_token:
        raise HTTPException(status_code=401, detail="Invalid API token")


async def get_spotify_access_token() -> str:
    spotify_client_id = os.getenv("SPOTIFY_CLIENT_ID", "")
    spotify_client_secret = os.getenv("SPOTIFY_CLIENT_SECRET", "")

    if not spotify_client_id or not spotify_client_secret:
        raise HTTPException(status_code=500, detail="Spotify credentials are not configured")

    if "access_token" in _token_cache:
        return _token_cache["access_token"]

    basic = base64.b64encode(f"{spotify_client_id}:{spotify_client_secret}".encode()).decode()

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(
            "https://accounts.spotify.com/api/token",
            headers={"Authorization": f"Basic {basic}"},
            data={"grant_type": "client_credentials"},
        )
        if response.status_code != 200:
            raise HTTPException(status_code=502, detail="Failed to obtain Spotify access token")

        payload = response.json()
        token = payload.get("access_token")
        if not token:
            raise HTTPException(status_code=502, detail="Spotify token response missing access token")

        _token_cache["access_token"] = token
        return token


@app.get("/health")
async def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/cover", response_model=CoverResponse, dependencies=[Depends(verify_api_token)])
async def cover(trackId: str = Query(..., min_length=10)):
    token = await get_spotify_access_token()

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(
            f"https://api.spotify.com/v1/tracks/{trackId}",
            headers={"Authorization": f"Bearer {token}"},
        )
        if response.status_code == 401:
            _token_cache.pop("access_token", None)
            token = await get_spotify_access_token()
            response = await client.get(
                f"https://api.spotify.com/v1/tracks/{trackId}",
                headers={"Authorization": f"Bearer {token}"},
            )

        if response.status_code != 200:
            raise HTTPException(status_code=502, detail="Failed to fetch Spotify track data")

        track = response.json()
        album = track.get("album", {})
        images = album.get("images", [])
        if not images:
            raise HTTPException(status_code=404, detail="No album art found for this track")

        best = max(images, key=lambda item: item.get("width", 0) or 0)
        artist_name = None
        artists = track.get("artists", [])
        if artists:
            artist_name = artists[0].get("name")

        return CoverResponse(
            trackId=trackId,
            imageUrl=best.get("url", ""),
            width=best.get("width"),
            height=best.get("height"),
            albumName=album.get("name"),
            artistName=artist_name,
        )

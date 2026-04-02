# 🎵 DynamicLock Backend

A small FastAPI server that securely exchanges your Spotify credentials for album covers.

## What It Does

1. Receives a Spotify track ID from the Android app
2. Uses your Spotify Client ID/Secret to fetch track metadata
3. Returns the highest-quality album art URL
4. Caches access tokens to avoid rate limits

## Setup

### Prerequisites
- Python 3.8+
- pip and venv

### Installation

```bash
# Create virtual environment
python3 -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Copy and configure environment
cp .env.example .env

# Edit .env with your Spotify credentials
nano .env
```

### Environment Variables

```env
SPOTIFY_CLIENT_ID=your_client_id_from_spotify_dashboard
SPOTIFY_CLIENT_SECRET=your_client_secret_from_spotify_dashboard
BACKEND_API_TOKEN=optional_token_for_api_protection
```

- **SPOTIFY_CLIENT_ID**: From [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
- **SPOTIFY_CLIENT_SECRET**: Keep this secret — never commit it
- **BACKEND_API_TOKEN**: Optional. If set, the Android app must include this header in requests

### Running Locally

```bash
python -m uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
```

The server will be available at `http://localhost:8000`

#### API Endpoints

**Health Check**
```bash
curl http://localhost:8000/health
# Returns: {"status":"ok"}
```

**Get Album Cover**
```bash
curl "http://localhost:8000/cover?trackId=3n3Ppam7vgaVa1iaRUc9Lp"

# Response:
{
  "trackId": "3n3Ppam7vgaVa1iaRUc9Lp",
  "imageUrl": "https://i.scdn.co/image/...",
  "width": 640,
  "height": 640,
  "albumName": "Album Name",
  "artistName": "Artist Name"
}
```

With auth token:
```bash
curl -H "X-API-Token: your_token" "http://localhost:8000/cover?trackId=3n3Ppam7vgaVa1iaRUc9Lp"
```

**Interactive Docs**
```
http://localhost:8000/docs
```
Browse SwaggerUI to test endpoints interactively.

## Deployment (Later)

Production deployment guides for Railway, Render, Fly.io, and Docker coming soon.

## Architecture

This backend is kept minimal intentionally:
- Stateless — can be replicated horizontally
- Fast — caches Spotify tokens in memory
- Simple — ~120 lines of code
- Secure — credentials never leave the server

## Security Notes

- Never commit `.env` to git
- Keep `SPOTIFY_CLIENT_SECRET` confidential
- Consider adding rate limiting for production
- Use HTTPS only in production

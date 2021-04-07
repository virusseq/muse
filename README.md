# Muse

Molecular Upload Submission sErvice

# DEV environment setup
A docker-compose setup is available with all required services for MUSE in `./compose`.

## Run:
 Move to compose folder: `cd compose`
 
 Start containers: `docker compose up -d`
 
 Initialize minio: `sh ./init.sh`
 
Note: Sometimes SONG-server might fail to start if DB is not ready yet; restart should fix it

## Using DEV setup:         
JWT with READ/WRITE scopes for SONG, score and MUSE (DOMAIN) can be generated from ego with:

`curl -X POST "http://localhost:8081/oauth/token?client_id=admin&client_secret=adminSecret&grant_type=client_credentials" -H "accept: application/json"`

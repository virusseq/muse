# Muse

Molecular Upload Submission sErvice

# DEV environment setup
This app depends on [Overture Aria](https://github.com/overture-stack/aria), which is distributed from the GitHub Maven registry. You may need to create a "[Personal Access Token (classic)](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic)", and to define it in a `settings.xml` file, in order for MUSE to compile. 

The following template can be used like `mvnw -s settings.xml clean package`, etc.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>GitHub_UserName</username>
            <password>Personal_Access_Token_Here</password>
        </server>
    </servers>
</settings>
```

A docker-compose setup is also available with all required services for MUSE in `./compose`.

## Run:
 Move to compose folder: `cd compose`
 
 Initialize dependency services: `./init-dep.sh`
 
Note: Sometimes SONG-server might fail to start if DB is not ready yet; restart should fix it

## Using DEV setup:         
JWT with READ/WRITE scopes for SONG, score and MUSE (DOMAIN) can be generated from ego with:

`curl -X POST "http://localhost:8081/oauth/token?client_id=admin&client_secret=adminSecret&grant_type=client_credentials" -H "accept: application/json"`

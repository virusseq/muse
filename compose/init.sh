echo Make default test bucket
docker run --rm \
    --network host \
    -e 'AWS_ACCESS_KEY_ID=minio' \
    -e 'AWS_SECRET_ACCESS_KEY=minio123' \
    -e 'AWS_DEFAULT_REGION=us-east-1' \
    mesosphere/aws-cli:latest \
    s3 --endpoint-url http://localhost:8085 mb s3://oicr.icgc.test;

echo Make data folder
docker run --rm \
    --network host \
    -v $(pwd)/object-storage-init:/data \
    -e 'AWS_ACCESS_KEY_ID=minio' \
    -e 'AWS_SECRET_ACCESS_KEY=minio123' \
    -e 'AWS_DEFAULT_REGION=us-east-1' \
    mesosphere/aws-cli:latest \
    s3 --endpoint-url http://localhost:8085 cp /data/heliograph s3://oicr.icgc.test/data/heliograph;

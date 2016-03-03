mvn clean package
export PHOTO_META_STAT_VERSION=$(cat pom.xml| grep version | head -n 1 | cut -d ">" -f 2 | cut -d "<" -f 1)
rm ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}.jar
mkdir ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}
mv ./target/original-photo-meta-stats-${PHOTO_META_STAT_VERSION}.jar ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}/photo-meta-stats-${PHOTO_META_STAT_VERSION}.jar
mv ./target/lib ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}/
mv ./target/run.bat ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}/
mv ./target/run.sh ./target/photo-meta-stats-${PHOTO_META_STAT_VERSION}/
cd target
zip -r photo-meta-stats-${PHOTO_META_STAT_VERSION}.zip photo-meta-stats-${PHOTO_META_STAT_VERSION}


#!/bin/bash

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC_DIR="$BASE_DIR/src/main/java"
LIB_JAR="$BASE_DIR/lib/servlet-api.jar"
JACKSON_DIR="$BASE_DIR/lib/jackson"
BIN_DIR="$BASE_DIR/bin"
DIST_DIR="$BASE_DIR/dist"
JAR_NAME="ProcessRequest.jar"

CP="$LIB_JAR:$JACKSON_DIR/jackson-databind-2.17.0.jar:$JACKSON_DIR/jackson-core-2.17.0.jar:$JACKSON_DIR/jackson-annotations-2.17.0.jar"

rm -rf "$BIN_DIR" "$DIST_DIR"
mkdir -p "$BIN_DIR" "$DIST_DIR"

echo "-> Compilation des sources Java..."
javac -cp "$CP" -d "$BIN_DIR" $(find "$SRC_DIR" -name "*.java")

if [ $? -ne 0 ]; then
    echo "[Erreur] La compilation a échoué."
    exit 1
fi

echo "-> Inclusion de Jackson dans le jar..."
cd "$BIN_DIR"
for jar in "$JACKSON_DIR"/*.jar; do
    unzip -q -o "$jar" -x "META-INF/*"
done
cd "$BASE_DIR"

echo "-> Création du fichier JAR..."
jar -cf "$DIST_DIR/$JAR_NAME" -C "$BIN_DIR" .

echo "-> JAR disponible ici : $DIST_DIR/$JAR_NAME"

# DIAR-E

Multi-module Java 21 + JavaFX app for mood/achievement logging with audio diary and WALL-E inspired energy meter.

## Modules
- diar-core: domain model
- diar-application: application services and ports
- diar-adapter-persistence-sqlite: SQLite persistence (skeleton)
- diar-adapter-audio: Java Sound audio adapter (skeleton)
- diar-ui-desktop: JavaFX UI
- diar-bootstrap: entrypoint and wiring

## Build
```
mvn -U -q -DskipTests package
```

## Run (UI)
```
cd diar-ui-desktop
mvn -q javafx:run
```

## Run (Bootstrap main)
```
mvn -q -pl diar-bootstrap -am exec:java -Dexec.mainClass=dev.diar.bootstrap.Main
```

## Notes
- If you see errors about unresolved JavaFX BOM, run with `-U` to force Maven to refresh dependencies: `mvn -U ...`
```

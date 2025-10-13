# DIAR-E

A WALL-E inspired multi-module Java 21 + JavaFX application for mood and achievement logging with audio diary capabilities.

**DIAR-E was inspired by WALL-E** - how he keeps going no matter what, and still does something every day, block by block, building towers of blocks.

## Features

- **Achievement Categories**: Create custom categories for tracking different types of achievements
- **Block System**: Add blocks to categories, building towers as you make progress
- **Tower Completion**: Automatically creates new towers when you reach the target number of blocks
- **Audio Diary**: Record voice notes and reflections with real-time audio level monitoring
- **SQLite Persistence**: All data stored locally in SQLite database

## Architecture

DIAR-E follows a **hexagonal architecture** (ports and adapters):

```
diar-e/
├── diar-core/                    # Domain models (Category, Tower, LogEntry, Recording)
├── diar-application/             # Business logic and port interfaces
├── diar-adapter-persistence-sqlite/  # SQLite repository implementations
├── diar-adapter-audio/           # Java Sound API audio capture
├── diar-ui-desktop/              # JavaFX user interface
└── diar-bootstrap/               # Main entry point, wires everything together
```

## Requirements

- **Java 21** or higher
- **Maven 3.8+**
- Working microphone (for audio diary feature)

## Building

From the project root:

```bash
mvn clean install
```

## Running

After building, run the application:

```bash
mvn -f src/bootstrap/diar-bootstrap/pom.xml exec:java
```

Or from the bootstrap directory:

```bash
cd src/bootstrap/diar-bootstrap
mvn exec:java
```

## Usage

### First Launch

When you first launch DIAR-E:
1. The app will create `~/.diar-e/` directory for data storage
2. SQLite database will be initialized with the schema
3. An empty main view will be displayed

### Creating Categories

1. Click **"+ New Category"** button
2. Enter a name (e.g., "Exercise", "Reading", "Coding")
3. Set a tower block target (how many blocks complete a tower)
4. Click OK

### Adding Blocks

1. Find your category card in the main view
2. Click **"+ Add Block"**
3. Optionally add a note about what you achieved
4. Click OK

Each block represents one achievement. When you reach the tower target, a new tower automatically starts!

### Audio Diary

1. Click **"🎤 Audio Diary"** button
2. Click **"⏺ Start Recording"** to begin
3. Speak your thoughts and reflections
4. Watch the audio level meter in real-time
5. Click **"⏹ Stop Recording"** when done
6. Recording is saved as WAV file in `~/.diar-e/recordings/`

## Project Structure

### Core Module (`diar-core`)
- **Domain models**: `Category`, `Tower`, `LogEntry`, `Recording`, `EnergyStatus`, `Block`
- Pure Java, no dependencies

### Application Module (`diar-application`)
- **Port interfaces**: Repository and adapter interfaces
- **Services**: `CategoryService`, `BlockService`, `RecordingService`, `MorningRoutineService`
- Business logic layer

### Adapters

**Persistence (`diar-adapter-persistence-sqlite`)**:
- SQLite repository implementations
- Flyway migrations for schema management
- HikariCP connection pooling

**Audio (`diar-adapter-audio`)**:
- Java Sound API integration
- Real-time audio capture
- WAV file generation
- Audio level meter

### UI Module (`diar-ui-desktop`)
- JavaFX desktop interface
- WALL-E inspired visual theme
- Views: `MainView`, `CategoryCard`, `RecordingDialog`
- `ApplicationContext` for dependency injection

### Bootstrap Module (`diar-bootstrap`)
- Main entry point
- Wires all components together
- Initializes database and creates repositories

## Data Storage

All data is stored in `~/.diar-e/`:
- **data/diar.db**: SQLite database (categories, towers, logs, recordings metadata, settings)
- **recordings/**: Audio diary WAV files

## Tech Stack

- **Java 21**: Records, sealed types, pattern matching
- **JavaFX 21**: Desktop UI framework
- **SQLite + JDBC**: Local database
- **HikariCP**: Connection pooling
- **Flyway**: Database migrations
- **Maven**: Build and dependency management
- **Java Sound API**: Audio recording

## Development

The project uses Maven's multi-module structure. Each module has its own `pom.xml` with dependencies clearly defined.

### Module Dependencies

```
diar-bootstrap
  └─→ diar-ui-desktop
  └─→ diar-adapter-persistence-sqlite
  └─→ diar-adapter-audio

diar-ui-desktop
  └─→ diar-application

diar-adapter-persistence-sqlite
  └─→ diar-application

diar-adapter-audio
  └─→ diar-application

diar-application
  └─→ diar-core

diar-core
  (no dependencies)
```

## Future Enhancements

- Tower visualization (stacked blocks UI)
- Daily energy tracking
- Morning routine feature
- Statistics and charts
- Data export/import
- Dark/light theme toggle
- Audio playback in UI

## License

MIT License - See LICENSE file for details

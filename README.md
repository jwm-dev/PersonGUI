# Person Manager Application

A modern, cross-platform Java desktop application for managing large collections of people, with rich metadata, advanced filtering, and a modular, themeable GUI. 

## Features

- **Modular GUI**: Four main modules—List, Viewer, Filter, and Terminal—provide a complete workflow for managing person data.
- **Rich Metadata**: Each person entry supports names, OCCCDate-based date of birth, government/student IDs, descriptions, and tags.
- **Live Retheming**: Instantly switch between several included themes without restarting the app.
- **Advanced Filtering**: Powerful, user-friendly filter module with custom filter saving and exporting.
- **Terminal Module**: Command-line interface for batch operations, scripting, and power-user features.
- **Import from Wikipedia**: Built-in script to import lists of people from Wikipedia (e.g., computer scientists).
- **Data Export**: Export filtered lists to .ppl, .json, or .txt formats.
- **Cross-Platform**: Runs on any OS with Java 17+ (tested on Linux).

## Modules Overview

### List Module
- Displays all people in a sortable, filterable table.
- Supports selection, viewing, and integration with the filter module.
- Click a row to view/edit details in the Viewer module.

### Viewer Module
- View and edit details for a selected person.
- Add, update, or delete entries.
- Edit metadata: description and tags.
- Date of birth entry with calendar popup and live date format switching.
- All changes are reflected instantly in the List module.

### Filter Module
- Search and filter people by name, ID, DOB, and more.
- Save and reuse custom filters.
- Export filtered results to .ppl, .json, or .txt.
- User-friendly interface with live updates.

### Terminal Module
- Command-line interface for advanced/batch operations.
- Supports listing, finding, adding, editing, and deleting people.
- File operations (ls, cd, cat, grep, etc.) within the data directory.
- Scripting support and configuration commands.
- Fun easter egg: play the Star Wars ASCII movie.

## Theming
- Choose from several built-in themes.
- Instantly apply new themes from the Tools menu or via the terminal (`set THEME <name>`).
- All modules update live, including popups and dialogs.

## Getting Started

### Prerequisites
- Java 17 or newer (OpenJDK or Oracle JDK)

### Build & Run
1. Download or clone this repository.
2. Compile:
   ```
   javac PersonApp.java
   ```
3. Run:
   ```
   java PersonApp
   ```

No additional setup is required. All data is stored in the `data/` directory.

## Importing People from Wikipedia
- Use the built-in Wikipedia Import tool (Tools → Import from Wikipedia...) to fetch and import lists of people (e.g., computer scientists) directly into your database.

## Who is this for?
- Anyone who needs to manage large amounts of person data: researchers, educators, archivists, or hobbyists.
- Designed for both casual users and power users (with the terminal module).

## Authors
- Jeffrey
- Damon

## License
This project is provided as-is for educational and personal use.

---

*For more information, see the in-app Help menu or About dialog.*

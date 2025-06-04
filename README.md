# 📦 ListRemoteZip

## Description

This program reads a remote `.zip` file over HTTP and lists all files inside it (with full internal paths) **without downloading the entire archive**.  
It uses HTTP range requests to fetch only the End of Central Directory (EOCD) and Central Directory sections.

> ⚠️ Limitations:
> - Does not process the .ZIP file comment at the end of the archive
> - Does not support files larger than 4 GB
> - Intended to be run from the console

---

## ⚙️ Requirements

- Java **17** or higher

---

## 🚀 How to run

1. Clone or download the project.
2. Open a terminal in the root project folder.
3. Compile the program:
    
   ```sh
    javac src/main/java/ListRemoteZip.java
    ```
4. Run the program:
    
   ```sh
    java -cp src/main/java ListRemoteZip <URL>.zip
    ```
   
5. **Example:**
    
   ```sh
    java -cp src/main/java ListRemoteZip https://sample-files.com/downloads/compressed/zip/mixed-files.zip
    ```

> No additional parameters are needed beyond the .zip URL.

---

## 🧪 Testing

- The project includes unit tests under `src/test/java`.
- Sample zip files for testing: [https://sample-files.com/misc/zip/](https://sample-files.com/misc/zip/)

---

© 2025 github.com/dmitri-kramar

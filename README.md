# Cloud Storage Downloader:
It supports `AWS S3` and `Google Cloud Storage` at the moment.

### Steps:
 1. Update `src/main/resources/.env` and `src/main/resources/demo_10k_svc_account_key.json` to include the credentials required for AWS and GCP access. 
 2. Run `Maven` `reload` task to download the dependencies
 3. Run the `main` method in `src/main/java/com.zhuor.cloud.downloader.demo/main.java` 
 4. Check the `download` directory in the project root

The expected example file format is in a CSV format:
```csv
name, github
John, @lzhuor
```

## MIT License
```
Copyright (c) 2022 Zhuoran LI <mail at zhuoran.li>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the 'Software'), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

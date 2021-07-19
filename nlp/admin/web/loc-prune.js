/*
 * Copyright (C) 2017/2021 e-voyageurs technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* This script removes localization entries that come from node modules */

const fs = require('fs')
const parser = require('fast-xml-parser');


fs.readFile('src/locale/messages.xlf', 'utf8' , (err, xmlData) => {
  if (err) {
    console.error(err);
    return;
  }
  try {
    const jsonObj = parser.parse(xmlData, {
      attrNodeName: "__attr",
      attributeNamePrefix: '',
      ignoreAttributes: false
    }, true);
    const body = jsonObj["xliff"]["file"]["body"];
    body["trans-unit"] = body["trans-unit"]
      .filter(transUnit => {
        console.log(JSON.stringify(transUnit));
        let ctxGroups = transUnit["context-group"];
        if (!Array.isArray(ctxGroups)) {
          ctxGroups = [ctxGroups];
        }
        return !ctxGroups.every(ctxGroup => ctxGroup["context"]
          .some(c => c["__attr"]["context-type"] === "sourcefile" && c["#text"].startsWith("../node_modules/")));
      });
    const j2xParser = new parser.j2xParser({
      format: true,
      attrNodeName: "__attr",
      ignoreAttributes: false,
      attributeNamePrefix: '',
      supressEmptyNode: true
    });
    const xml = j2xParser.parse(jsonObj);
    fs.writeFile('src/locale/messages1.xlf', xml, err => console.log(err));
  } catch(error) {
    console.error(error);
  }
});

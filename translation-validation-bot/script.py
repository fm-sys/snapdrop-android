import os
import github

from googletrans import Translator
import requests
import re

# extracting all the input from environments
INPUT_PR_NUMBER = os.environ['INPUT_PR']

print("PR number: " + INPUT_PR_NUMBER)

printcache=""
def print2cache(str):
    global printcache
    printcache += str
    printcache += "\n"

url = 'https://github.com/fm-sys/snapdrop-android/pull/' + INPUT_PR_NUMBER + '.diff'
r = requests.get(url + "", allow_redirects=True)
text = r.content.decode()

translator = Translator()

table_initialized = False

for line in text.splitlines():
    if line.startswith("+"):
        match = re.search("<string.*?name=\"(.+?)\".*?>(.*)</string>", line)
        if match:
            if not table_initialized:
                table_initialized = True
                print2cache("ID|Translation|Reverse translated source string\n-|-|-")
            translation = translator.translate(match.group(2).replace("\\n", " \\n "))
            print2cache(f"{match.group(1)}|{translation.origin} ({translation.src})|{translation.text} ({translation.dest})")
        elif line.startswith("+++"):
            print2cache("\n\n" + line + "\n")
            table_initialized = False


print("::set-output name=content::"+printcache.replace("%", "%25").replace("\n", "%0D").strip())

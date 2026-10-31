#!/usr/bin/env python3
"""Temporary diagnostic: inspect publicly served Freesound previews, no downloads."""
import re
import urllib.request
import html
pages = [
 "https://freesound.org/people/MellowAudio/sounds/591486/",
 "https://freesound.org/people/SpliceSound/sounds/260124/",
 "https://freesound.org/people/Jofae/sounds/362954/",
]
for url in pages:
 try:
  req=urllib.request.Request(url,headers={"User-Agent":"Mozilla/5.0"})
  with urllib.request.urlopen(req,timeout=15) as response:
   content=response.read(2000000).decode("utf-8","replace")
  print("PROBE URL", url, "pagebytes",len(content))
  for match in re.finditer(r"(?:cdn\.freesound\.org|/data/previews/|previews/|preview-hq-mp3|preview-lq-mp3)",content,re.I):
   print("PROBE MATCH",html.unescape(content[max(0,match.start()-100):match.start()+200]).replace("\n"," ")[:300])
   if match.start()>250000:break
  urls=re.findall(r'https?[^\s"<>]{10,180}(?:mp3|ogg)',content,re.I)
  print("PROBE URLS",urls[:8])
 except Exception as exc:print("PROBE ERROR",type(exc).__name__,str(exc)[:250])

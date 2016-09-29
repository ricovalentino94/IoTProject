import time
import picamera
import base64
import paho.mqtt.client as mqtt
import random, string
import math
import json
from time import sleep

def capture():
  camera = picamera.PiCamera()

  try:
    camera.start_preview()
    sleep(1)
    camera.capture('image.jpg', resize=(500,281))
    camera.stop_preview()
    pass
  finally:
    camera.close()

def convertImageToBase64():
  with open("image.jpg", "rb") as image_file:
    encoded = base64.b64encode (image_file.read())
  return encoded

#client = mqtt.Client()
#client.connect("localhost",1883,60)

def randomword(length):
  return ''.join(random.choice(string.lowercase) for i in range (length))

packet_size=3000

def publishEncodedImage(encoded):
  end = packet_size
  start = 0
  length = len(encoded)
  picId = randomword(8)
  pos = 0
  no_of_packets = math.ceil(length/packet_size)

  while start <= len(encoded) :
    data = {"data": encoded[start:end], "end":"0", "pos": pos, "size": no_of_packets}
    client.publish("toAndroid",json.JSONEncoder().encode(data))
    time.sleep(0.05)
    end += packet_size
    start += packet_size
    pos = pos + 1
  data = {"data": encoded[start:end], "end":"1", "pos": pos, "size":no_of_packets}
  client.publish("toAndroid",json.JSONEncoder().encode(data))
  client.disconnect()

while True:
  capture()
  client = mqtt.Client()
  client.connect("localhost",1883,60)
  encoded2=convertImageToBase64()
  publishEncodedImage(encoded2)

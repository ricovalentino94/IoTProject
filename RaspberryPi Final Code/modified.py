import paho.mqtt.client as mqtt

import RPi.GPIO as GPIO
import time
import serial

import thread

from threading import Thread

import picamera
import base64

import random, string
import math
import json
from time import sleep

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

statusCamera = False
awayStatus = False
def on_connect(client, userdata, flags, rc):
  print("Connected with result code "+str(rc))
  client.subscribe("toRPI")

def on_message(client, userdata, msg):
  if (msg.payload == "home"):
    print("home!")
    global awayStatus 
    awayStatus = False
    
  elif (msg.payload == "away"):
    print("away!")
    global awayStatus 
    awayStatus = True
    t = Thread(target=detectIntruder, args=())
    t.start()

  elif (msg.payload == "cctvON"):
    print "cctvON"
    global statusCamera
    statusCamera = True
    t = Thread(target=callCamera, args=())
    t.start()

  elif (msg.payload == "cctvOFF"):
    global statusCamera
    statusCamera = False
   
def detectIntruder():     
  while awayStatus and not(statusCamera):        
         DETECT_PERSON=GPIO.input(11)
         if DETECT_PERSON==0:
           print "No intruders",DETECT_PERSON
           
           temp = ser.readline()
           print "temp0",temp[0]
           if (len(temp) == 4 ):
             print "LAMP IS ON"
             client = mqtt.Client()
             client.connect("localhost",1883,60)
             client.publish("toAndroid","lamp")
           elif (len(temp) == 5):
             print "LAMP IS OFF"
           time.sleep(1)
         elif DETECT_PERSON==1:               #When output from motion sensor i$
           print "Intruder detected",DETECT_PERSON
           client = mqtt.Client()
           client.connect("localhost",1883,60)
           client.publish("toAndroid","intruder") 
           time.sleep(3)
def capture():
    global camera
    print("masuk capture")
   # camera = picamera.PiCamera()
    try:
      #camera.sart_preview()
      sleep(1)
      camera.capture('image.jpg', resize=(500,281))
      #camera.stop_preview()
      pass
    finally:
      camera.close()

def callCamera():
   print "aku kamera"
   global camera
   camera = picamera.PiCamera()
   while statusCamera:
     print "masuk statuscam"
     capture()
     client = mqtt.Client()
     client.connect("localhost",1883,60)
     encoded2=convertImageToBase64()
     publishEncodedImage(encoded2)

DETECT_PERSON = 0 
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BOARD)
GPIO.setup(11, GPIO.IN)         #Read output from PIR motion sensor
ser = serial.Serial('/dev/ttyACM0',9600)

client = mqtt.Client()
client.connect("localhost",1883,60)

client.on_connect = on_connect
client.on_message = on_message

client.loop_forever()

#include <SoftwareSerial.h>

SoftwareSerial BTSerial(10,11);
boolean received = false;
String receivedData = "";
String sentData = "";

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  Serial.println("Hello!");

  BTSerial.begin(9600);  
}

void loop() {
  // put your main code here, to run repeatedly:
  if(BTSerial.available()){
      char charR = BTSerial.read();
      receivedData+=charR;
      received = true;
      delay(10);
  }

  if(!BTSerial.available() && received){
      Serial.print("Received : ");
      Serial.println(receivedData);
      received = false;
      receivedData="";
  }
  
  if(Serial.available()){
    char toSend = Serial.read();
    if(toSend!='\n'){
          BTSerial.write(toSend);
          sentData+=toSend;

    }else{
        Serial.write("Sent : ");
        Serial.println(sentData);
        sentData="";
    }
  }
}

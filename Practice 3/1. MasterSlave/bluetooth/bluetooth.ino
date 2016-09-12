#include <SoftwareSerial.h>

SoftwareSerial mySerial(10,11);



void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  while(!Serial){
    ;
  }
  
  Serial.println("Goodnight moon!");
  Serial.println("SET BT NAME Arduino_BT");
  mySerial.begin(4800);
  mySerial.println("Hello,world?");
  
}

void loop() {
  // put your main code here, to run repeatedly:
  if(mySerial.available()){
    Serial.write(mySerial.read());
  }
  if(Serial.available()){
    mySerial.write(Serial.read());
    
  }
}

//#include <SHT1x.h>

//#define DATAPIN 12
//#define CLOCKPIN 13
//SHT1x sht1x(DATAPIN, CLOCKPIN);        //I2C pin set

unsigned int g_unPhotoSensorValue = 0;
unsigned int g_unCdsSensorValue = 0;
//float g_fTempC = 0;
//float g_fHumidity = 0;

void setup()
{
    Serial.begin(115200);
    Serial.println("======Sensor Read Start========");
}

//void TempHumiSensorRead()
//{
    //g_fTempC = sht1x.readTemperatureC() - 5;        //Temp Read
    //g_fHumidity = sht1x.readHumidity();         //Humi Read
//}

void PhotoSensorRead()
{
    g_unPhotoSensorValue = analogRead(A0);
}
void CdsSensorRead()
{
    g_unCdsSensorValue = analogRead(A1);
}

void loop()
{
    //TempHumiSensorRead();
    PhotoSensorRead();
    CdsSensorRead();
    //Serial.print("TEMP = ");
    //Serial.println(g_fTempC);
    //Serial.print("HUMI = ");
    //Serial.println(g_fHumidity);
    Serial.print("Photo Sensor Value = ");
    Serial.println(g_unPhotoSensorValue);
    Serial.print("CDS Sensor Value = ");
    Serial.println(g_unCdsSensorValue);
    delay(500);
}

/********************************************************************

 파 일 : main.c
 
 동 작 : 
	Sensor 모듈의 온/습도 센서 SHT11에서 센싱한 아날로그 정보를 디지털로
	변환한 수치를 획득하여 2-Wire 직렬 버스 인터페이스를 통하여 MCU로 전송,
	전송된 정보를 온/습도를 계산하여 TEXT LCD 를 통하여 디스플레이

 포트연결 :
	AVR 모듈의 포트 J5(PORT B)를 Text LCD 모듈의 포트 LCD_CD에 연결
	AVR 모듈의 포트 J6(PORT E)를 Text LCD 모듈의 포트 LCD_DB에 연결
	AVR 모듈의 포트 J7(PORT F)를 Sensor 모듈의 포트 TEMP에 연결

 소스 설명 :
 1) 센서 제어 관련 헤더파일 포함
 2) LCD 구동 관련 헤더파일 포함
 3) 온/습도 값 저장 변수 선언
 4) 온/습도 값을 LCD에 표시
 5) 온/습도 센서 초기 설정
 6) LCD 초기 설정
 7) LCD 문자열 표시
 8) Move into 2nd Line
 9) 220us 지연
 10) LCD 문자열 표시
 11) 500ms 지연 
 12) Clear Display
 13) 9ms 지연
 14) Sensing Temp
 15) Sensing Humi
 16) Cursor At Home(84ms delay)
 17) 온도 값을 LCD에 표시
 18) Move into 2nd Line
 19) 습도 값을 LCD에 표시

********************************************************************/

#include "lib_sensor.h"

volatile float temp;
volatile float humi;

int main (void)
{
	int nCdsChannel = 0;
	int nPhotoCellChannel = 1;
	
	int nCdsValue = 0;
	int nPhotoCellValue = 0;
	
	if(wiringPiSetupGpio() == -1) {
		fprintf(stdout,"Not start wiringPi: %s\n",strerror(errno));
		return 1;
	}
	
	if(wiringPiSPISetup(SPI_CHANNEL, SPI_SPEED) == -1) {
		fprintf(stdout, "wiringPiSPISetup Failed: %s\n", strerror(errno));
		return 1;
	}
	
	pinMode(CS_MCP3208, OUTPUT);

	SHT11_Init();
	
	while (1)
	{
		Transmission_start();
		temp = get_SHT11_data (TEMP);	// Sensing Temp
		delay(100);

		Transmission_start();
		humi = get_SHT11_data (HUMI); 	// Sensing Humi
		delay(100);

		printf("Temp = %2.2f [C], Humi = %2.2f [%]\n", temp, humi);

		nCdsValue = ReadMcp3208ADC(nCdsChannel);				//sensorRead
		nPhotoCellValue = ReadMcp3208ADC(nPhotoCellChannel);	//sensorRead
		printf("CSD Sensor Value = %u\n", nCdsValue);
		printf("Photocell Sensor Value = %u\n", nPhotoCellValue);
		delay(500);
	}
	return 0;
}
	

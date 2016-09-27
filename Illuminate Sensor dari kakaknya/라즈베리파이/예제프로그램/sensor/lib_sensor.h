/********************************************************************

 �� �� : lcd.h

 �ҽ� ���� :
 1) ����� ���� ������� ����
 2) Delay ���� ������� ����
 3) ���ͷ�Ʈ ���� ������� ����
 4) �µ��� ������ ��Ÿ���� ���ڿ� ���� ����
 5) ��/���� ������ I2C ���� �Լ��� ������Ÿ���� ����
 
********************************************************************/

#ifndef	__LIB_SENSOR_H__
#define	__LIB_SENSOR_H__

#include <stdio.h>
#include <string.h>
#include <errno.h>

#include <wiringPi.h>
#include <wiringPiSPI.h>

#define CS_MCP3208	8			//GPIO 8
#define SPI_CHANNEL	0			//SPI Channel
#define SPI_SPEED	1000000		//spi speed

#define SET_DATA()	digitalWrite(SDA, 1)
#define CLR_DATA()	digitalWrite(SDA, 0)
#define SET_SCK()	digitalWrite(SCK, 1)
#define CLR_SCK()	digitalWrite(SCK, 0)
#define READ_DATA()	digitalRead(SDA)
#define READ_SCK()	digitalRead(SCK)

#define	SCK		6
#define	SDA		12

#define NOACK	0
#define ACK   	1
                            		// Addr	    Code(command)   r/w
#define MEASURE_TEMP		0x03	// 000   	0001  			1
#define MEASURE_HUMI		0x05	// 000   	0010  			1
#define READ_STATUS_REG		0x07	// 000   	0011  			1
#define WRITE_STATUS_REG	0x06	// 000   	0011  			0
#define RESET				0x1e   	// 000   	1111  			0

enum { TEMP, HUMI };

void SHT11_Init (void);
void Connection_reset (void);
void Transmission_start (void);
float get_SHT11_data (unsigned char type);
unsigned char Write_byte (unsigned char value);
unsigned char Read_byte (unsigned char ack);
unsigned char Measure (unsigned short *p_value, 
						unsigned short *p_checksum,	unsigned char mode);
void calc_SHT11 (unsigned short p_humidity ,unsigned short p_temperature);

int ReadMcp3208ADC(unsigned char adcChannel);

#endif	/* __LIB_SENSOR_H__ */

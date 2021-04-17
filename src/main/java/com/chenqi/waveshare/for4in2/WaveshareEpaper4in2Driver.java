package com.chenqi.waveshare.for4in2;

import com.pi4j.io.gpio.*;
import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

public class WaveshareEpaper4in2Driver {

    public final static int WIDTH = 400;
    public final static int HEIGHT = 300;

    private WaveshareEpaper4in2Driver() {
    }

    private static WaveshareEpaper4in2Driver waveshareEpaper4in2Driver = new WaveshareEpaper4in2Driver();

    public static WaveshareEpaper4in2Driver getInstance() {
        return waveshareEpaper4in2Driver;
    }

    final static GpioPinDigitalOutput DC;
    final static GpioPinDigitalOutput RST;
    final static GpioPinDigitalInput BUSY;
    // SPI device
    public static SpiDevice spi;

    static {
        // in order to use the Broadcom GPIO pin numbering scheme, we need to configure the
        // GPIO factory to use a custom configured Raspberry Pi GPIO provider
        RaspiGpioProvider raspiGpioProvider = new RaspiGpioProvider(RaspiPinNumberingScheme.BROADCOM_PIN_NUMBERING);
        GpioFactory.setDefaultProvider(raspiGpioProvider);

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        DC = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_25, "DC", PinState.HIGH);
        RST = gpio.provisionDigitalOutputPin(RaspiBcmPin.GPIO_17, "RST", PinState.HIGH);
        BUSY = gpio.provisionDigitalInputPin(raspiGpioProvider, RaspiBcmPin.GPIO_24, "BUSY");

        try {
            spi = SpiFactory.getInstance(SpiChannel.CS0, //驱动板CS引脚连树莓派CE0，所以这里指定CS0的SpiChannel
                    SpiDevice.DEFAULT_SPI_SPEED, // default spi speed 1 MHz
                    SpiDevice.DEFAULT_SPI_MODE); // default spi mode 0
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * reset墨水屏
     *
     * @throws InterruptedException
     */
    private void reset() throws InterruptedException {
        System.out.println("reset spi");
        RST.high();
        Thread.sleep(200);
        RST.low();
        Thread.sleep(10);
        RST.high();
        Thread.sleep(200);
    }

    /**
     * 写入指令
     *
     * @param date
     * @throws IOException
     */
    private void sendCommand(int date) throws IOException {
        DC.low();
        spi.write((byte) date);
    }

    /**
     * 写入数据
     *
     * @param date
     * @throws IOException
     */
    private void sendData(int date) throws IOException {
        DC.high();
        spi.write((byte) date);
    }

    /**
     * 判断屏幕是否忙
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void readBusy() throws IOException, InterruptedException {
        System.out.println("readBusy spi");
        sendCommand((byte) 0x71);
        while (BUSY.isLow()) {
            sendCommand((byte) 0x71);
            Thread.sleep(1000);
            System.out.println("BUSY!!!");
        }
        System.out.println("not busy!");
    }

    /**
     * 初始化屏幕
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void init() throws IOException, InterruptedException {
        System.out.println("init spi");
        reset();

        sendCommand(0x01); //Power Setting
        sendData(0x03);
        sendData(0x00);
        sendData(0x2b);
        sendData(0x2b);
        sendData(0x09);//python驱动中未写入该值，但是数据手册中写入了该值

        sendCommand(0x06); //Booster Soft Start
        sendData(0x17);
        sendData(0x17);
        sendData(0x17);

        sendCommand(0x04); //Power ON
        readBusy();

        sendCommand(0x00); //Panel Setting
        sendData(0xbf);
        //sendData(0x0d); //python驱动中多写入了0d,不知道是何用意

        sendCommand(0x30); //PLL control
        sendData(0x3c); //3A 100HZ   29 150Hz 39 200HZ  31 171HZ

        sendCommand(0x61); //Resolution setting
        sendData(0x01);
        sendData(0x90);
        sendData(0x01);
        sendData(0x2c);

        sendCommand(0x82); //VCM_DC Setting
        sendData(0x28);

        sendCommand(0X50); //Vcom and data interval setting
        sendData(0x97);

        setLut(); //设置LUT
    }

    /**
     * 初始化LUT
     *
     * @throws IOException
     */
    private void setLut() throws IOException {
        sendCommand(0x20);
        for (int data : LUT_VCOM0) {
            sendData(data);
        }

        sendCommand(0x21);
        for (int data : LUT_WW) {
            sendData(data);
        }

        sendCommand(0x22);
        for (int data : LUT_BW) {
            sendData(data);
        }

        sendCommand(0x23);
        for (int data : LUT_BB) {
            sendData(data);
        }

        sendCommand(0x24);
        for (int data : LUT_WB) {
            sendData(data);
        }

    }

    /**
     * 树莓派驱动墨水屏显示
     *
     * @param srcImg
     * @throws IOException
     * @throws InterruptedException
     */
    public void display(Image srcImg) throws IOException, InterruptedException {
        System.out.println("just display.no gray.");
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        Graphics graphics = bufferedImage.getGraphics();

        //将原始位图按墨水屏幕大小缩小后绘制到bufferedImage对象中
        graphics.drawImage(srcImg, 0, 0, WIDTH, HEIGHT, null);

        //sendCommand(0x92); //Partial Out,python驱动中有设置这个，奇怪，设置局部刷新？这个指令不设置也是可以的
        final byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        System.out.println(" pixels size = " + pixels.length);

        setLut();
        sendCommand(0x10);
        for (int i = 0; i < HEIGHT * WIDTH / 8; i++) {
            sendData(0xFF);
        }
        sendCommand(0x13);
        for (int i = 0; i < HEIGHT * WIDTH / 8; i++) {
            sendData(pixels[i]);
        }
        sendCommand(0x12);
        readBusy();
    }

    /**
     * 树莓派驱动墨水屏显示
     *
     * @param destImg
     * @throws IOException
     * @throws InterruptedException
     */
    public void display2(BufferedImage destImg) throws IOException, InterruptedException {
        final byte[] pixels = ((DataBufferByte) destImg.getRaster().getDataBuffer()).getData();
        System.out.println(" pixels size = " + pixels.length);
        //sendCommand(0x92);
        setLut();
        sendCommand(0x10);
        for (int i = 0; i < HEIGHT * WIDTH / 8; i++) {
            sendData(0xFF);
        }
        sendCommand(0x13);
        for (int i = 0; i < HEIGHT * WIDTH / 8; i++) {
            sendData(pixels[i]);
        }
        sendCommand(0x12);
        readBusy();
    }

    /**
     * 清屏，相当于所有像素设置1：白。。记住：0黑1白
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void clear() throws IOException, InterruptedException {
        //sendCommand(0x92);
        setLut();
        sendCommand(0x10);
        for (int i = 0; i < HEIGHT * WIDTH; i++) {
            sendData(0xFF);
        }
        sendCommand(0x13);
        for (int i = 0; i < HEIGHT * WIDTH; i++) {
            sendData(0xFF);
        }
        sendCommand(0x12);
        readBusy();
    }

    final static int[] LUT_VCOM0 = {0x00, 0x17, 0x00, 0x00, 0x00, 0x02,
            0x00, 0x17, 0x17, 0x00, 0x00, 0x02,
            0x00, 0x0A, 0x01, 0x00, 0x00, 0x01,
            0x00, 0x0E, 0x0E, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final static int[] LUT_WW = {0x40, 0x17, 0x00, 0x00, 0x00, 0x02,
            0x90, 0x17, 0x17, 0x00, 0x00, 0x02,
            0x40, 0x0A, 0x01, 0x00, 0x00, 0x01,
            0xA0, 0x0E, 0x0E, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final static int[] LUT_BW = {0x40, 0x17, 0x00, 0x00, 0x00, 0x02,
            0x90, 0x17, 0x17, 0x00, 0x00, 0x02,
            0x40, 0x0A, 0x01, 0x00, 0x00, 0x01,
            0xA0, 0x0E, 0x0E, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final static int[] LUT_WB = {0x80, 0x17, 0x00, 0x00, 0x00, 0x02,
            0x90, 0x17, 0x17, 0x00, 0x00, 0x02,
            0x80, 0x0A, 0x01, 0x00, 0x00, 0x01,
            0x50, 0x0E, 0x0E, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final static int[] LUT_BB = {0x80, 0x17, 0x00, 0x00, 0x00, 0x02,
            0x90, 0x17, 0x17, 0x00, 0x00, 0x02,
            0x80, 0x0A, 0x01, 0x00, 0x00, 0x01,
            0x50, 0x0E, 0x0E, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
}

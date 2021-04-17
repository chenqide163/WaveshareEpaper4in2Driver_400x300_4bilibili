package com.chenqi.waveshare.for4in2;

import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Date;
import java.util.Random;

public class DrawImg {
    private static Logger LOG = Logger.getLogger(DrawImg.class);

    public static void main(String[] args) throws IOException {

        //读取原始位图
        Image srcImage = ImageIO.read(new File("D:\\test.jpg"));
        BufferedImage destImg = getFloydSteinbergBinImg(srcImage);
        ImageIO.write(destImg, "jpg", new File("D:\\test2.jpg"));

        destImg = getTitleImg();
        ImageIO.write(destImg, "jpg", new File("D:\\title.jpg"));
    }

    /**
     * 获取灰阶抖动的图片，使用Floyd–Steinberg dithering算法
     * 参考https://en.wikipedia.org/wiki/Floyd%E2%80%93Steinberg_dithering
     *
     * @param srcImage 返回的TYPE_BYTE_BINARY 二值图片
     * @return
     * @throws IOException
     */
    public static BufferedImage getFloydSteinbergBinImg(Image srcImage) throws IOException {
        int width = WaveshareEpaper4in2Driver.WIDTH;
        int height = WaveshareEpaper4in2Driver.HEIGHT;

        //定义一个BufferedImage对象，用于保存缩小后的灰阶图片
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = bufferedImage.getGraphics();

        //将原始位图按墨水屏幕大小缩小后绘制到bufferedImage对象中
        graphics.drawImage(srcImage, 0, 0, width, height, null);

        BufferedImage destImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        int color[][] = new int[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 取出灰度图片各像素灰阶值，256级灰阶，即 & 0xff 作用
                color[x][y] = bufferedImage.getRGB(x, y) & 0xff;
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int oldPixel = color[x][y];
                int newPixel = oldPixel > 125 ? 255 : 0; //应为需要转化为2级灰阶，即非255即0，在灰阶中，0黑1白

                if (0 == newPixel) {
                    destImg.setRGB(x, y, 0);
                } else {
                    destImg.setRGB(x, y, 0xffffff);
                }

                int quantError = oldPixel - newPixel;

                //右边像素
                if (x + 1 < width) {
                    int pointPixel = color[x + 1][y] + quantError * 7 / 16;
                    color[x + 1][y] = pointPixel;
                }

                //左下像素
                if (x > 0 && y + 1 < height) {
                    int pointPixel = color[x - 1][y + 1] + quantError * 3 / 16;
                    color[x - 1][y + 1] = pointPixel;
                }

                //下像素
                if (y + 1 < height) {
                    int pointPixel = color[x][y + 1] + quantError * 5 / 16;
                    color[x][y + 1] = pointPixel;
                }

                //下右像素
                if (y + 1 < height && x + 1 < width) {
                    int pointPixel = color[x][y + 1] + quantError * 1 / 16;
                    color[x][y + 1] = pointPixel;
                }
            }
        }
        return destImg;
    }

    /**
     * 获取灰阶图片的字节数组
     *
     * @return
     * @throws IOException
     */
    public static BufferedImage getWeatherImg() throws IOException {
        LOG.debug("start to getFutureWeatherImg ");
        int width = WaveshareEpaper4in2Driver.WIDTH;
        int height = WaveshareEpaper4in2Driver.HEIGHT;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();

        //默认创建出来的图片是黑底的，设置为白底。0黑1白，故设置每个像素为0xffffff
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xffffff);
            }
        }

        //获取树莓派ip
        String piIp = getRaspiIP();
        g.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        g.setColor(new Color(0x0));
        g.drawString("IP:" + piIp, 0, 16);

        String date = "04-17 周六";
        String time = "11:46";
        g.setFont(new Font("微软雅黑", Font.PLAIN, 40));
        g.setColor(new Color(0x0));
        g.drawString(date, 0, 60);

        g.setFont(new Font("微软雅黑", Font.PLAIN, 72));
        g.setColor(new Color(0x0));
        g.drawString(time, 215, 62);

        //画上横线
        g.drawLine(0, 69, 400, 69);
        //画上竖线
        g.drawLine(205, 0, 205, 69);
        //画下竖线
        g.drawLine(205, 69, 205, 267);
        //画下横线
        g.drawLine(0, 267, 400, 267);

        //画右上横线
        g.drawLine(205, 135, 400, 135);
        //画右下横线
        g.drawLine(205, 201, 400, 201);

        String quality = "空气质量 : 优良";
        String temperature = "温度 : " + "8°C ~ 12°C";
        String notice = "愿你拥有比阳光更明媚的心情";

        g.setFont(new Font("微软雅黑", Font.BOLD, 40));
        g.drawString("南京", 32, 108);

        g.setFont(new Font("微软雅黑", Font.BOLD, 26));
        g.drawString("晴", 130, 110);

        String wind = "北风2级";
        g.setFont(new Font("微软雅黑", Font.PLAIN, 22));
        g.drawString(wind, 2, 137);
        g.drawString(quality, 2, 163);
        g.drawString(temperature, 2, 189);

        //画天气图标
        //将原始位图按墨水屏幕大小缩小后绘制到bufferedImage对象中
        g.drawImage(getResourceImg("/weatherIcon/闪电.jpg"), 20, 194, 70, 65, null);

        //画坐标 图标
        g.drawImage(getResourceImg("/weatherIcon/zuobiao.jpg"), 2, 72, 28, 40, null);

        //下雨天则画雨伞
        g.drawImage(getResourceImg("/weatherIcon/umbrella.jpg"), 100, 202, 50, 46, null);

        height = 69;
        g.drawImage(getResourceImg("/weatherIcon/umbrella.jpg"), 315, height + 3, 38, 38, null);
        g.drawString("周日" + " " + "雪", 210, height + 40);
        g.drawString(temperature, 210, height + 62);
        g.drawImage(getResourceImg("/weatherIcon/雪.jpg"), 360, height + 3, 40, 37, null);

        height = 135;
        g.drawString("周一" + " " + "阴", 210, height + 40);
        g.drawString(temperature, 210, height + 62);
        g.drawImage(getResourceImg("/weatherIcon/阴.jpg"), 360, height + 3, 40, 37, null);

        height = 201;
        g.drawImage(getResourceImg("/weatherIcon/umbrella.jpg"), 315, height + 3, 38, 38, null);
        g.drawString("周二" + " " + "闪电", 210, height + 40);
        g.drawString(temperature, 210, height + 62);
        g.drawImage(getResourceImg("/weatherIcon/闪电.jpg"), 360, height + 3, 40, 37, null);


        g.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g.drawString(notice, 2, 290);
        return image;
    }

    /**
     * 获取树莓派IP
     *
     * @return
     */
    private static String getRaspiIP() {
        InputStream in = null;
        BufferedReader read = null;
        try {
            String command = "hostname -I | cut -d' ' -f1";
            Process pro = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            pro.waitFor();
            in = pro.getInputStream();
            read = new BufferedReader(new InputStreamReader(in));
            String result = "";
            String line;

            while ((line = read.readLine()) != null) {
                result = result + line + "\n";
            }
            LOG.info("getRaspiIP is : " + result);
            return result;
        } catch (Exception e) {
            LOG.error(e);
            return "do not get the IP!";
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取资源图片
     *
     * @param imgPath
     * @return
     */
    private static BufferedImage getResourceImg(String imgPath) {
        DrawImg getLcdImg = new DrawImg();
        InputStream is = getLcdImg.getClass().getResourceAsStream(imgPath);
        BufferedImage fbImg = null;
        try {
            fbImg = ImageIO.read(is);
        } catch (IOException e) {
            try {
                is.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        return fbImg;
    }

    /**
     * 制作B站标题图
     *
     * @return
     * @throws IOException
     */
    public static BufferedImage getTitleImg() {
        LOG.debug("start to getTitleImg ");
        int width = WaveshareEpaper4in2Driver.WIDTH;
        int height = WaveshareEpaper4in2Driver.HEIGHT;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = image.createGraphics();

        //默认创建出来的图片是黑底的，设置为白底。0黑1白，故设置每个像素为0xffffff
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xffffff);
            }
        }
        g.setFont(new Font("华文新魏", Font.BOLD, 60));
        g.setColor(new Color(0));
        g.drawString("JAVA 玩转", 50, 52);
        g.drawString("树莓派 墨水屏", 4, 130);

        g.setFont(new Font("华文新魏", Font.PLAIN, 30));
        g.setColor(new Color(0));
        g.drawString("4.2寸 微雪", 0, 200);
        g.drawString("分辨率400x300", 0, 240);
        g.drawString("陈琦玩派派", 240, 290);
        return image;
    }
}

package com.chenqi.waveshare.for4in2;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Epaper4in2Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        WaveshareEpaper4in2Driver.getInstance().init();
        WaveshareEpaper4in2Driver.getInstance().clear();
        WaveshareEpaper4in2Driver.getInstance().display(DrawImg.getWeatherImg());
        Thread.sleep(2000);
        //showPicsFromSameFolder();
    }

    /**
     * 获取jar包同路径下的所有后缀为jpg的图片并展示
     */
    public static void showPicsFromSameFolder() throws IOException, InterruptedException {
        String path = Epaper4in2Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.substring(0, path.lastIndexOf(File.separator) + 1);
        System.out.println(path);
        File file = new File(path);
        File[] files = file.listFiles();
        for (File eachFile : files) {
            if (eachFile.getName().toLowerCase().endsWith("jpg")) {
                String imgPath = eachFile.getCanonicalPath();
                System.out.println("imgPath = " + imgPath);
                BufferedImage srcImage = DrawImg.getFloydSteinbergBinImg(ImageIO.read(new File(imgPath)));
                WaveshareEpaper4in2Driver.getInstance().display2(srcImage);
                Thread.sleep(2000);
            }

        }
    }
}

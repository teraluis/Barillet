import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.util.Scanner;
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        System.out.println("nom de l'image avec son extension ex postal.png:");
        String str = sc.nextLine();
        System.out.println("chemin : postals/" + str);
        BufferedImage image = ImageIO.read(new File("postals/"+str));
        // parameters for correction
        double paramA = 0.017715; // n'affecte que les pixels le plus à l'exterieur de l'image 0.02 equivalent au point A
        double paramB = 0.026731; // la plus part de ces cas ne nécessitent qu'une optimisation b 0.02 equivalent au point B
        double paramC = 0.026731; // most uniform correction 0.05 0.026731 equivalent au point A'
        double paramD = 1.0 - paramA - paramB - paramC; // décrit la mise à l'échelle linéaire de l'image  equivalent au point B'
        int width = image.getWidth();
        int height = image.getHeight();
        double total = width*height;
        double progresion = 0;
        double pourcent = 0;
        int[] pixels = new int[width * height];
        //int[] pixelsCopy = pixels.clone();
        // Retrieve pixel info and store in 'pixels' variable
        PixelGrabber pgb = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        try {
            pgb.grabPixels();
            writeTextFile("src/raw.txt", pixels, width);
            // It's supposed that user modifies pixels file here
            int d = Math.min(width, height) / 2;    //calcul du rayon du cercle
            //System.out.println("rayon du cercle : "+d+"px");
            // centre de l'image
            double centerX = (width - 1) / 2.0;
            double centerY = (height - 1) / 2.0;
            //System.out.println("Point central de l'image ["+centerX+"px,"+centerY+"px]");
            int[] pixelsCopy = readTextFile("src/raw.txt", width, height);
            //System.out.println("debut de la boucle for");
            for(int i =0; i< width ; i++) {
                for(int j =0; j< height ; j++) {
                    // cordonnées cartesiennes du vecteur par rapport au centre de l'image
                    double deltaX = (i - centerX) / d;
                    double deltaY = (j - centerY) / d;
                    //System.out.println("delta ["+deltaX+"px,"+deltaY+"px]");
                    // distance ou rayon de l'image
                    double dstR = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                    //System.out.println("distance :"+dstR);
                    // distance ou rayon de l'image avec les parametres correctionels
                    double srcR = (paramA * dstR * dstR * dstR + paramB * dstR * dstR + paramC * dstR + paramD) * dstR;

                    // coefficient directionel de fresnel http://www.chimix.com/an8/cap8/cap87.htm
                    double factor = Math.abs(dstR / srcR);

                    // cordonnées dans l'image source en utilisant le coefficent de fresnel
                    double srcXd = centerX + (deltaX * factor * d);
                    double srcYd = centerY + (deltaY * factor * d);

                    // on arrondi au point le plus proche
                    int srcX = (int) srcXd;
                    int srcY = (int) srcYd;
                    //calcul de la position du vecteur considerez que width = 1 pour simplifier
                    int dstPos = j * width + i;
                    //System.out.println("position "+ dstPos);
                    //on obtien les cordonnées finales (x,y) considerez que width = 1 tjs pour mieux comprendre
                    pixels[dstPos] = pixelsCopy[srcY * width + srcX];

                    progresion = progresion+1;

                    progressBar(progresion,total,pourcent);
                    pourcent = Math.round((progresion/total)*100);

                }
            }
            //on convertit la matrice en image
            textToImage("transformed/"+str, width, height, pixels);
            Scanner sc2 = new Scanner(System.in);
            System.out.println("voulez vous retourner l'image");
            boolean rotate = sc.nextBoolean();
            if(rotate) {
                BufferedImage transformed = ImageIO.read(new File("transformed/"+str));
                BufferedImage rotatedImage = rotateImage(transformed,180);
                ImageIO.write(rotatedImage, "png", new File("transformed/"+str));
                System.out.println("Image has been rotated");
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
    private static void writeTextFile(String path, int[] data, int width) throws IOException {
        FileWriter f = new FileWriter(path);
        // Write pixel info to file, comma separated
        for(int i = 0; i < data.length; i++) {
            String s = Integer.toString(data[i]);
            f.write(s + ", ");
            if (i % width == 0) f.write(System.lineSeparator());
        }
        f.close();
    }

    private static int[] readTextFile(String path, int width, int height) throws IOException {
        BufferedReader csv = new BufferedReader(new FileReader(path));
        int[] data = new int[width * height];

        // Retrieves array of pixels as strings
        String[] stringData = parseCSV(csv);

        // Converts array of pixels to ints
        for(int i = 0; i < stringData.length; i++) {
            String num = stringData[i];
            data[i] = Integer.parseInt(num.trim());
        }
        return data;
    }

    private static String[] parseCSV(BufferedReader csv) throws IOException {
        ArrayList<String> fileData = new ArrayList<>();
        String row;

        // Fulfills 'fileData' with list of rows
        while ((row = csv.readLine()) != null) fileData.add(row);

        // Joins 'fileData' values into single 'joinedRows' string
        StringBuilder joinedRows = new StringBuilder();
        for(String line : fileData) joinedRows.append(line);

        // Splits 'joinedRows' by comma and returns array of pixels
        return joinedRows.toString().split(", ");
    }
    private static void textToImage(String path, int width, int height, int[] data) throws IOException {
        MemoryImageSource mis = new MemoryImageSource(width, height, data, 0, width);
        java.awt.Image im = Toolkit.getDefaultToolkit().createImage(mis);

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(im, 0, 0, null);
        ImageIO.write(bufferedImage, "png", new File(path));
        System.out.println("\r\n100% Done! Check the result");
    }
    private static void  progressBar(double progresion, double total,double pourcent) throws IOException, InterruptedException {
        if(Math.round((progresion/total)*100)>pourcent){
            StringBuilder data = new StringBuilder("=");
            for (int a =0;a<pourcent;a++){
                data.append("=");
            }
            data.append(">");
            System.out.write(data.toString().getBytes());
            System.out.println("\r\n"+pourcent+"%");
        }
    }
    public static BufferedImage rotateImage(BufferedImage img, double angle) {

        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage dest = new BufferedImage(width,height,img.getType());
        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.translate(0,0);
        graphics2D.rotate((Math.PI/180)*180, width / 2, height /2);
        graphics2D.drawRenderedImage(img,null);
        return dest;
    }
}

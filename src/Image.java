import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.*;
import java.util.ArrayList;

public class Image {

    public double paramA = 0.017715;
    public double paramB = 0.026731;
    public double paramC = 0.026731;
    public double paramD = 1.0;
    public int width;
    public int height;
    public int total;
    public int[] pixels;
    public String name;
    public BufferedImage image;
    public PixelGrabber pgb;
    public double progresion = 0;
    public double pourcent = 0;
    public Image(String name) {
        this.name = name;
    }

    public Boolean readImage() throws IOException {
        this.image= ImageIO.read(new File("postals/"+this.name));
        this.width = this.image.getWidth();
        this.height = this.image.getHeight();
        this.total = this.width*this.height;
        this.pixels = new int[this.total];
        if(this.total == 0) {
            return false;
        }else {
            this.pgb = new PixelGrabber(this.image, 0, 0, this.width, this.height, this.pixels, 0, this.width);
            return true;
        }
    }

    public void generateImage() throws IOException, InterruptedException {
        writeTextFile("src/raw.txt", this.pixels, this.width);
        int d = Math.min(this.width, this.height) / 2;
        double centerX = (this.width - 1) / 2.0;
        double centerY = (this.height - 1) / 2.0;
        int[] pixelsCopy = readTextFile("src/raw.txt", this.width, this.height);
        for(int i =0; i< this.width ; i++) {
            for(int j =0; j< this.height ; j++) {
                double deltaX = (i - centerX) / d;
                double deltaY = (j - centerY) / d;
                double dstR = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double srcR = (this.paramA * dstR * dstR * dstR + this.paramB * dstR * dstR + this.paramC * dstR + this.paramD) * dstR;
                double factor = Math.abs(dstR / srcR);
                double srcXd = centerX + (deltaX * factor * d);
                double srcYd = centerY + (deltaY * factor * d);

                int srcX = (int) srcXd;
                int srcY = (int) srcYd;

                int dstPos = j * this.width + i;

                this.pixels[dstPos] = pixelsCopy[srcY * this.width + srcX];

                this.progresion = this.progresion+1;

                progressBar(this.progresion,this.total,this.pourcent);
                pourcent = Math.round((this.progresion/this.total)*100);

                textToImage("transformed/"+this.name, width, height, pixels);

            }
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

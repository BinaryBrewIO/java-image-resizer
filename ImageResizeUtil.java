/* ------------------------------------------------------------------------
 * ImageResizeUtil.java
 * ------------------------------------------------------------------------
 * 
 * Copyright © 2019 BinaryBrew authors
 * 
 * This source file may be used and distributed without restriction provided
 * that this copyright statement is not removed from the file and that any
 * derivative work contains the original copyright notice and the associated
 * disclaimer.
 *
 * This source file is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This source file is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the noasic library.  If not, see http://www.gnu.org/licenses
 * 
 * ------------------------------------------------------------------------ */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

class ImageResizeUtil {

    private static String IMAGE_TYPE = "jpg";
    private static String ORIGINAL_IMAGE = "./mk.jpg";

    private static final String ROOT_PATH = "./thumb/";
    private static final String RESIZE_FROM_CENTER_IMAGE_NAME = "thumb-resize-from-center";
    private static final String SQUARE_CENTERED_IMAGE_NAME = "thumb-square-centered";
    private static final String SQUARE_FULL_TRANSPARENT_IMAGE_NAME = "thumb-square-full-transparent";
    private static final String SQUARE_FULL_BGCOLOR_IMAGE_NAME = "thumb-square-full-bg-color";
    private static final String ASPECT_RATIO_IMAGE_NAME = "thumb-aspect-ratio";
    private static final String FIXED_WIDTH_IMAGE_NAME = "thumb-fixed-width";
    private static final String FIXED_HEIGHT_IMAGE_NAME = "thumb-fixed-height";
    private static final String STRETCHED_IMAGE_NAME = "thumb-stretched";

    private static final int SQUARE_SIZE = 256;

    public static void main(String[] args) {
        try {
            // file name from command line if provided
            if (args.length > 0) {
                String fileToProcess = args[0];
                if (fileToProcess != null) {
                    ORIGINAL_IMAGE = fileToProcess;
                }
            }
            
            File imageFile = new File(ORIGINAL_IMAGE);
            BufferedImage bufferImage = ImageIO.read(imageFile);
            String IMAGE_NAME_PREFIX = imageFile.getName().substring(0, imageFile.getName().lastIndexOf('.')) + '-';

            // resize image from center (cropped image), given size must be smaller than original file size
            BufferedImage resizeFromCenter = resizeFromCenter(bufferImage, 900, 450);
            saveImage(resizeFromCenter, IMAGE_TYPE, IMAGE_NAME_PREFIX + RESIZE_FROM_CENTER_IMAGE_NAME);

            // perfect square image from center of an image (cropped image)
            BufferedImage squreImage = resizeToSquare(bufferImage, SQUARE_SIZE, false, null, false);
            saveImage(squreImage, IMAGE_TYPE, IMAGE_NAME_PREFIX + SQUARE_CENTERED_IMAGE_NAME);

            // resized full image to square, considering aspect ratio & set its background to given color
            BufferedImage squreImageFC = resizeToSquare(bufferImage, SQUARE_SIZE, true, Color.WHITE, false);
            saveImage(squreImageFC, IMAGE_TYPE, IMAGE_NAME_PREFIX + SQUARE_FULL_BGCOLOR_IMAGE_NAME);

            // resize image in %
            BufferedImage imageWithAspectRatio = resizeToPercentageWithAspectRatio(bufferImage, 25);
            saveImage(imageWithAspectRatio, IMAGE_TYPE, IMAGE_NAME_PREFIX + ASPECT_RATIO_IMAGE_NAME);

            // resize image with fixed width
            BufferedImage imageWithFixedWidth = resizeToFixedWidthWithAspectRatio(bufferImage, 150);
            saveImage(imageWithFixedWidth, IMAGE_TYPE, IMAGE_NAME_PREFIX + FIXED_WIDTH_IMAGE_NAME);

            // resize image with fixed height
            BufferedImage imageWithFixedHeight = resizeToFixedHeightWithAspectRatio(bufferImage, 400);
            saveImage(imageWithFixedHeight, IMAGE_TYPE, IMAGE_NAME_PREFIX + FIXED_HEIGHT_IMAGE_NAME);

            // resize by stratching image (no one's gonna use it anyway)
            BufferedImage stretchedImage = resizeMe(bufferImage, SQUARE_SIZE, SQUARE_SIZE);
            saveImage(stretchedImage, IMAGE_TYPE, IMAGE_NAME_PREFIX + STRETCHED_IMAGE_NAME);

            // tranceparent image with full image resized
            IMAGE_TYPE = "png";
            BufferedImage squreImageFT = resizeToSquare(bufferImage, SQUARE_SIZE, true, null, true);
            saveImage(squreImageFT, IMAGE_TYPE, IMAGE_NAME_PREFIX + SQUARE_FULL_TRANSPARENT_IMAGE_NAME);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private static Rectangle getSquareDimension(BufferedImage bufferImage) {
        // default width & height from image
        int imgW = bufferImage.getWidth();
        int imgH = bufferImage.getHeight();
        // default starting point is from 0,0
        int startX = 0;
        int startY = 0;
        // default widht, height from image
        int newW = imgW;
        int newH = imgH;
        // consider smallest size for square
        if (imgW > imgH) {
            newW = imgH;
            // if width is more then X needs to move
            startX = (int) Math.ceil((imgW - newW) / 2);
        } else if (imgW < imgH) {
            newH = imgW;
            // if height is more then Y needs to move
            startY = (int) Math.ceil((imgH - newH) / 2);
        }
        return new Rectangle(startX, startY, newW, newH);
    }

    private static BufferedImage resizeToSquare(BufferedImage bufferImage, int imageSize, boolean fullImageInSquare, Color backgroundColor, boolean enableTransparency) {
        if (fullImageInSquare) {
            BufferedImage img = null;
            int xPos = 0;
            int yPos = 0;
            if (bufferImage.getWidth() > bufferImage.getHeight()) {
                img = resizeToFixedWidthWithAspectRatio(bufferImage, imageSize);
                yPos = (imageSize-img.getHeight())/2;
            } else if (bufferImage.getHeight() > bufferImage.getWidth()) {
                img = resizeToFixedHeightWithAspectRatio(bufferImage, imageSize);
                xPos = (imageSize-img.getWidth())/2;
            }
            if (img != null) {
                boolean isTransparency = (backgroundColor == null && enableTransparency);
                BufferedImage squareImage = new BufferedImage(imageSize, imageSize, (isTransparency)?BufferedImage.TYPE_INT_ARGB:img.getType());
                Graphics2D g2d = squareImage.createGraphics();
                if(!isTransparency) {
                    g2d.setPaint(backgroundColor);
                    g2d.fillRect(0, 0, imageSize, imageSize);
                }
                g2d.drawImage(img.getScaledInstance(img.getWidth(), img.getHeight(), Image.SCALE_SMOOTH), xPos, yPos, null);
                g2d.dispose();
                return squareImage;
            }
            return null;
        }
        // get a square from the center of an image
        Rectangle imgDimension = getSquareDimension(bufferImage);
        // crop image of square dimension
        BufferedImage croppedImg = bufferImage.getSubimage(imgDimension.x, imgDimension.y, imgDimension.width, imgDimension.height);
        // reduce the size of the square as needed & return
        return resizeMe(croppedImg, imageSize, imageSize);
    }

    private static BufferedImage resizeFromCenter(BufferedImage bufferImage, int width, int height) {
        if (bufferImage.getHeight() < height || bufferImage.getWidth() < width) {
            // image is smaller than resize values
            System.out.println("Image is smaller than expected!");
            return null;
        }

        int xPos = (bufferImage.getWidth()-width)/2;
        int yPos = (bufferImage.getHeight()-height)/2;
        return bufferImage.getSubimage(xPos, yPos, width, height);
    }

    private static BufferedImage resizeToPercentageWithAspectRatio(BufferedImage bufferImage, double percent) {
        int scaledWidth = (int) (bufferImage.getWidth() * (percent/100));
        int scaledHeight = (int) (bufferImage.getHeight() * (percent/100));
        return resizeMe(bufferImage, scaledWidth, scaledHeight);
    }

    private static BufferedImage resizeToFixedWidthWithAspectRatio(BufferedImage bufferImage, double maxWidth) {
        int scaledHeight = (int) Math.ceil((maxWidth*bufferImage.getHeight()) / bufferImage.getWidth());
        return resizeMe(bufferImage, (int) maxWidth, scaledHeight);
    }

    private static BufferedImage resizeToFixedHeightWithAspectRatio(BufferedImage bufferImage, double maxHeight) {
        int scaledWidth = (int) Math.ceil((maxHeight*bufferImage.getWidth()) / bufferImage.getHeight());
        return resizeMe(bufferImage, scaledWidth, (int) maxHeight);
    }

    private static BufferedImage resizeMe(BufferedImage img, int width, int height) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, img.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private static void saveImage(BufferedImage bufferedImage, String imageType, String filePath) {
        try {
            File directory = new File(ROOT_PATH);
            if (! directory.exists()){
                directory.mkdir();
            }
            File outputfile = new File(ROOT_PATH + filePath + '.' + imageType);
            ImageIO.write(bufferedImage, imageType, outputfile);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
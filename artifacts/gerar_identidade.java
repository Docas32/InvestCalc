// Gera a identidade visual do InvestCalc (P7.3, paleta WhatsApp) sem
// dependência externa: usa o Java2D do JDK já usado pela build.
//
//   - artifacts/icone_512.png          (512x512, gradiente 135° + glifo)
//   - artifacts/feature_graphic.png    (1024x500, gradiente + glifo +
//                                       wordmark "InvestCalc" + tagline)
//
// Uso:  javac gerar_identidade.java && java -Djava.awt.headless=true gerar_identidade
//
// As unidades 0..108 espelham o viewport dos vetores do launcher
// (ic_launcher_foreground.xml / ic_launcher_background.xml).

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class gerar_identidade {

    static final Color VERDE_CLARO = new Color(0x25D366); // topo-esquerda
    static final Color VERDE_ESCURO = new Color(0x075E54); // baixo-direita
    static final Color BRANCO = new Color(0xFFFFFFFF);

    // ---- Glifo em unidades 0..108 (idêntico ao XML do launcher) ----
    static final double[] B1 = {32, 53, 42, 77};
    static final double[] B2 = {50, 37, 60, 77};
    static final double[] B3 = {68, 23, 78, 77};
    static final double R = 4.0;

    static Path2D.Double rrect(double[] r) {
        double x0 = r[0], y0 = r[1], x1 = r[2], y1 = r[3];
        double w = x1 - x0, h = y1 - y0;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0 + R, y0);
        p.lineTo(x0 + w - R, y0);
        p.quadTo(x1, y0, x1, y0 + R);
        p.lineTo(x1, y0 + h - R);
        p.quadTo(x1, y1, x1 - R, y1);
        p.lineTo(x0 + R, y1);
        p.quadTo(x0, y1, x0, y1 - R);
        p.lineTo(x0, y0 + R);
        p.quadTo(x0, y0, x0 + R, y0);
        p.closePath();
        return p;
    }

    static Path2D.Double curve() {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(32, 70);
        p.curveTo(48, 54, 62, 40, 78, 28);
        return p;
    }

    static Path2D.Double arrowhead() {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(77, 31);
        p.lineTo(69, 32);
        p.lineTo(74, 39);
        p.closePath();
        return p;
    }

    static void desenharGlifo(Graphics2D g) {
        // g já está com translate/scale aplicados; desenha em unidades 0..108.
        g.setColor(BRANCO);
        g.fill(rrect(B1));
        g.fill(rrect(B2));
        g.fill(rrect(B3));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(curve());
        g.fill(arrowhead());
    }

    static void aplicarFundo(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, VERDE_CLARO, w, h, VERDE_ESCURO));
        g.fillRect(0, 0, w, h);
    }

    static void glifoEscala(Graphics2D g, double escala, double tx, double ty) {
        g.translate(tx, ty);
        g.scale(escala, escala);
        desenharGlifo(g);
    }

    static void salvar(BufferedImage img, String caminho) throws Exception {
        ImageIO.write(img, "png", new File(caminho));
        System.out.println("OK " + caminho + " (" + img.getWidth() + "x" + img.getHeight() + ")");
    }

    public static void main(String[] args) throws Exception {
        RenderingHints hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // ---- icone_512.png (512x512) ----
        int S = 512;
        BufferedImage icone = new BufferedImage(S, S, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = icone.createGraphics();
        g.setRenderingHints(hints);
        aplicarFundo(g, S, S);
        glifoEscala(g, S / 108.0, 0, 0);
        g.dispose();
        salvar(icone, "icone_512.png");

        // ---- feature_graphic.png (1024x500) ----
        int W = 1024, H = 500;
        BufferedImage fg = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D f = fg.createGraphics();
        f.setRenderingHints(hints);
        aplicarFundo(f, W, H);

        // Glifo: caixa de unidades x 32..81, y 23..77 (altura ~162dp na escala 3).
        double escala = 3.4;
        double centroGlifoY = (23 + 77) / 2.0 * escala;
        java.awt.geom.AffineTransform tf = f.getTransform();
        glifoEscala(f, escala,
                W / 2.0 - ((32 + 81) / 2.0) * escala,
                H / 2.0 - centroGlifoY - 40);
        f.setTransform(tf); // texto em coordenadas do canvas (não do glifo)

        Font bold = Font.createFont(Font.TRUETYPE_FONT, new File(
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")).deriveFont(96f);
        Font regular = Font.createFont(Font.TRUETYPE_FONT, new File(
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")).deriveFont(34f);

        FontRenderContext frc = f.getFontRenderContext();
        String wordmark = "InvestCalc";
        String tagline = "Calculadora CDI & Investimentos";

        int wordY = 388;
        int tagY = 458;

        f.setFont(bold);
        f.setColor(BRANCO);
        double wW = bold.getStringBounds(wordmark, frc).getWidth();
        f.drawString(wordmark, (float) ((W - wW) / 2), wordY);

        f.setFont(regular);
        double tW = regular.getStringBounds(tagline, frc).getWidth();
        f.drawString(tagline, (float) ((W - tW) / 2), tagY);

        f.dispose();
        salvar(fg, "feature_graphic.png");
    }
}
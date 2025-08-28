/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}
//DEPS com.pi4j:pi4j-core:3.0.1

import com.pi4j.Pi4J;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Example code to illustrate how a JavaFX UI can be executed with JBang.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * This example can be executed with:
 * jbang HelloJavaFXWorld.java
 */
public class HelloJavaFXWorld extends Application {

    private final int CANVAS_WIDTH = 400;
    private final int CANVAS_HEIGHT = 300;

    private final Random r = new Random();
    private Scene scene;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        var holder = new BorderPane();

        holder.setTop(getHeader());
        holder.setLeft(getInfo());
        holder.setCenter(getBouncingBalls());

        scene = new Scene(holder, 700, 400);
        stage.setTitle("JavaFX Demo");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Stage is closing");
    }

    private HBox getHeader() {
        Label headerLabel = new Label("JavaFX Demo");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        headerLabel.setTextFill(Color.WHITE);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: #2196F3; -fx-border-color: #1976D2; -fx-border-width: 0 0 2 0;");
        header.getChildren().add(headerLabel);

        return header;
    }

    private VBox getInfo() {
        var pi4j = Pi4J.newAutoContext();

        var holder = new VBox();
        holder.setFillWidth(true);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.setSpacing(5);
        holder.setPadding(new Insets(20));

        var osInfoHeader = new Label("OS");
        osInfoHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        osInfoHeader.setTextFill(Color.BLUE);
        holder.getChildren().add(osInfoHeader);

        var os = pi4j.boardInfo().getOperatingSystem();
        holder.getChildren().addAll(
                new Label(os.getName()),
                new Label(os.getVersion()),
                new Label(os.getArchitecture())
        );

        var javaInfoHeader = new Label("JAVA");
        javaInfoHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        javaInfoHeader.setTextFill(Color.BLUE);
        holder.getChildren().add(javaInfoHeader);

        var javaInfo = pi4j.boardInfo().getJavaInfo();
        holder.getChildren().addAll(
                new Label(javaInfo.getVersion()),
                new Label(javaInfo.getRuntime()),
                new Label(javaInfo.getVendor()),
                new Label(javaInfo.getVendorVersion())
        );

        var boardModelHeader = new Label("BOARD");
        boardModelHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        boardModelHeader.setTextFill(Color.BLUE);
        holder.getChildren().add(boardModelHeader);

        var boardModel = pi4j.boardInfo().getBoardModel();
        holder.getChildren().addAll(
                new Label(boardModel.getName()),
                new Label(boardModel.getLabel()),
                new Label(boardModel.getCpu().getLabel())
        );

        return holder;
    }

    private Canvas getBouncingBalls() {
        var canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);

        var context = canvas.getGraphicsContext2D();
        List<BallDrawing> bouncingBalls = new ArrayList<>();

        for (var i = 0; i < 10; i++) {
            bouncingBalls.add(new BallDrawing());
        }

        // Start the animation
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(5), t -> onTick(context, bouncingBalls)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        return canvas;
    }

    private void onTick(GraphicsContext context, List<BallDrawing> bouncingBalls) {
        // Clear the canvas (remove all the previously balls that were drawn)
        context.clearRect(0.0, 0.0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Fill the entire canvas with a background color
        context.setFill(Color.LIGHTBLUE);
        context.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Move all the balls in the list, and draw them on the Canvas
        for (BallDrawing ballDrawing : bouncingBalls) {
            ballDrawing.move();
            context.setFill(ballDrawing.getFill());
            context.fillOval(ballDrawing.getX(), ballDrawing.getY(), ballDrawing.getSize(), ballDrawing.getSize());
        }
    }

    class BallDrawing {
        private final Color fill;
        private final int size;
        private double x;
        private double y;
        private double moveX;
        private double moveY;

        public BallDrawing() {
            // Random fill color
            fill = Color.color(Math.random(), Math.random(), Math.random());

            // Random size
            size = r.nextInt(10, 30);

            // Random starting position within the canvas
            x = r.nextInt(CANVAS_WIDTH - size);
            y = r.nextInt(CANVAS_HEIGHT - size);

            // Random move direction, these will be reverted if the ball hits the edge of the canvas
            moveX = r.nextInt(-5, 5);
            moveY = r.nextInt(-5, 5);
        }

        public void move() {
            if (hitLeftOrRightEdge()) {
                moveX *= -1; // Ball hit right or left wall, so reverse direction
            }
            if (hitTopOrBottom()) {
                moveY *= -1; // Ball hit top or bottom, so reverse direction
            }
            x += moveX;
            y += moveY;
        }

        private boolean hitLeftOrRightEdge() {
            return (x < size) || (x > (CANVAS_WIDTH - size));
        }

        private boolean hitTopOrBottom() {
            return (y < size) || (y > (CANVAS_HEIGHT - size));
        }

        public Color getFill() {
            return fill;
        }

        public int getSize() {
            return size;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }
    }
}

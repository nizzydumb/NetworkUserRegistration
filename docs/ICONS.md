# Icon usage and import guide

## 1. Icons currently used

The project uses two kinds of icons.

### Application and window icon

The blue network-and-shield artwork is project-owned raster artwork created for this application. It is not loaded
from an icon-library dependency. The PNG resources are stored in `src/assets`:

```text
app-icon.png
app-icon-16.png
app-icon-24.png
app-icon-32.png
app-icon-48.png
app-icon-64.png
app-icon-128.png
app-icon-256.png
```

JavaFX receives all available sizes and lets Windows select the most appropriate one for the title bar, taskbar,
and window switcher. The loading code is in `MainWindow.setWindowIcon` and `AuthWindow.WindowIcons.apply`.

### Edit and delete button icons

The edit and delete symbols are small, hand-coded `SVGPath` shapes in `MainWindow.createEditIconButton` and
`MainWindow.createDeleteIconButton`. They do not require an external JAR, font, or network connection. Their colors,
line widths, hover states, and button sizes are defined by `.edit-icon`, `.delete-icon`, and `.row-action-button` in
`src/ui/app.css`.

## 2. Importing a PNG icon

Put application resources under `src`. The build copies every non-Java file from `src` into the output directory.
For example:

```text
src/assets/export-24.png
```

Load it from the classpath, not with an absolute filesystem path:

```java
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

URL iconUrl = MainWindow.class.getResource("/assets/export-24.png");
if (iconUrl == null) {
    throw new IllegalStateException("Missing icon resource: /assets/export-24.png");
}

ImageView icon = new ImageView(new Image(iconUrl.toExternalForm()));
icon.setFitWidth(18);
icon.setFitHeight(18);
icon.setPreserveRatio(true);

Button exportButton = new Button("Export", icon);
```

The leading `/` means the path starts at the classpath root. Do not use paths such as `C:\...`, because they will
break on another computer.

For a window icon, provide several square PNG sizes where possible:

```java
for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
    URL url = MainWindow.class.getResource("/assets/app-icon-" + size + ".png");
    if (url != null) {
        stage.getIcons().add(new Image(url.toExternalForm()));
    }
}
```

Recommended PNG properties:

- square canvas;
- transparent background when appropriate;
- exact pixel dimensions for small title-bar variants;
- readable silhouette at 16×16 and 24×24;
- no important detail against the outermost pixels.

## 3. Importing SVG path data

JavaFX `SVGPath` accepts the contents of an SVG element's `d` attribute, not an entire SVG document:

```xml
<svg viewBox="0 0 24 24">
    <path d="M5 12h14 M12 5v14"/>
</svg>
```

Use only the `d` value in Java:

```java
SVGPath icon = new SVGPath();
icon.setContent("M5 12h14 M12 5v14");
icon.getStyleClass().add("add-icon");

Button addButton = new Button("Add");
addButton.setGraphic(icon);
addButton.setAccessibleText("Add");
```

Then style it in `src/ui/app.css`:

```css
.add-icon {
    -fx-fill: transparent;
    -fx-stroke: #ffffff;
    -fx-stroke-width: 2;
    -fx-stroke-line-cap: round;
    -fx-stroke-line-join: round;
}
```

Prefer icons designed for a 24×24 view box so they remain visually consistent with the current edit and delete
symbols. If an SVG contains several paths, gradients, masks, text, or filters, convert it to a simple combined path
or export it as a PNG instead of copying the complete SVG markup into `SVGPath`.

## 4. Adding an icon to an existing button

Keep icon creation in a small helper when it is reused:

```java
private SVGPath createRefreshIcon() {
    SVGPath icon = new SVGPath();
    icon.setContent("M20 6v6h-6 M4 18v-6h6 M6.5 8a7 7 0 0 1 11-1.5 M17.5 16a7 7 0 0 1-11 1.5");
    icon.getStyleClass().add("refresh-icon");
    return icon;
}
```

For an icon-only button, always provide accessibility information:

```java
Button refreshButton = new Button();
refreshButton.setGraphic(createRefreshIcon());
refreshButton.setAccessibleText("Refresh");
refreshButton.setFocusTraversable(false);
```

Use `Tooltip` as well when the meaning may not be obvious to a sighted user.

## 5. Licensing and provenance

Before importing an icon from a website, package, design file, or icon set:

1. Confirm that its license permits use and redistribution in this project.
2. Do not assume that an image-search result is free to use.
3. Record the icon-set name, original URL, author, license, download date, and any modifications.
4. Copy required attribution or license text into `docs/licenses` if the license requires it.
5. Avoid mixing icons with noticeably different stroke widths, corner styles, or view-box sizes.

Add future entries to this table:

| Asset or class | Origin | License | Modifications |
|---|---|---|---|
| `assets/app-icon*.png` | Created for Network User Registration | Project-owned | Exported at multiple sizes |
| `MainWindow` edit path | Hand-coded geometric path | Project-owned | Styled through JavaFX CSS |
| `MainWindow` delete path | Hand-coded geometric path | Project-owned | Styled through JavaFX CSS |

If the origin or license cannot be established, do not commit the icon. Create a replacement or use an icon set
with clear redistribution terms.

## 6. Verification checklist

After adding an icon:

1. Run **Build | Rebuild Project** in IntelliJ.
2. Confirm there is no `Missing icon resource` error.
3. Check normal, hovered, pressed, focused, and disabled states.
4. Test at Windows display scaling such as 100%, 125%, and 150%.
5. Verify that an icon-only button has `accessibleText` and, where useful, a tooltip.
6. Confirm the resource is included by Git and is not accidentally hidden by `.gitignore`.

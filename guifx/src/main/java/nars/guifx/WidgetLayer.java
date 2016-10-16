package nars.guifx;

import br.com.supremeforever.mdi.MDICanvas;
import br.com.supremeforever.mdi.MDIWindow;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import nars.NAR;
import nars.guifx.demo.NARide;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static javafx.application.Platform.runLater;

/**
 * Manages the activated set of plugins in a NAR, and a menu for adding additional ones
 * and presets of them.
 */
public class WidgetLayer extends MDICanvas {

    final Map<String, Node> nodes = new ConcurrentHashMap<>();

    private final NAR nar;
    private final NARide ide;


    public WidgetLayer(NARide ide) {
        super();

        //super(Orientation.HORIZONTAL, itemSpacing, itemSpacing);

        maxWidth(Double.MAX_VALUE);
        maxHeight(Double.MAX_VALUE);
        //setPrefWrapLength(0);


        //super(Orientation.HORIZONTAL, itemSpacnig, itemSpacnig);
        //setgasetSpacing(itemSpacing);


        this.ide = ide;
        nar = ide.nar;


        nar.onFrame((n) -> runLater(this::update));

        runLater(this::update);


    }


    public void update() {

//        Map<String, Object> ss = nar.getSingletons();
//
//        List<Node> toAdd = $.newArrayList(ss.size());
//        ss.forEach((k, v) -> toAdd.add(node(k, v)));

//        //TODO use faster comparison method
//
//        if (!getChildren().equals(toAdd))
//            runLater(() -> {
//                getChildren().setAll(toAdd);
//                layout();
//            });


//        menu.add(new JLabel(" + "));
//
//        TreeMap<String, JMenu> menus = new TreeMap();
//        try {
//            TreeSet<Class> plugins = new TreeSet<>(new Comparator<Class>() {
//                @Override public int compare(Class o1, Class o2) {
//                    return o1.getSimpleName().compareTo(o2.getSimpleName());
//                }
//            });
//            plugins.addAll(PackageUtility.getClasses("nars.operate", false));
//            for (Class c : plugins) {
//                if (!IOperator.class.isAssignableFrom(c))
//                    continue;
//
//                String[] p = c.getPackage().getName().split("\\.");
//                String category = p[1];
//                JMenu j = menus.get(category);
//                if (j == null) {
//                    j = new JMenu(category);
//                    menus.put(category, j);
//                }
//                JMenuItem x = newAddPluginItem(c);
//                j.add(x);
//            }
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(PluginPanel.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        for (JMenu j : menus.values()) {
//            menu.add(j);
//        }
//
    }


    private final Node node(String k, Object v) {
        Node n = nodes.computeIfAbsent(k, (K) -> {

            Node s = icon(K, v);

            runLater(()-> {
                MDIWindow w = new MDIWindow(k, new ImageView("mdi/restore.png"), k, s);
                ((Region)s).setPrefSize(128,128);
                ((Region)s).setMinSize(32,32);
                ((Region)s).setMaxSize(256,256);
                newWindow(w);

            });

            //Button p = new Button();
            //p.getStyleClass().add("plugin_button");
            //p.setGraphic(s);
            //p.setMaxWidth(Double.MAX_VALUE);
            //p.setMaxHeight(Double.MAX_VALUE);
            //p.maxHeight(100);
            //p.prefHeight(100);

            return s;
        });


        return n;
    }

    private Node icon(String k, Object v) {

        if (v instanceof FXIconPaneBuilder) {
            // instance implements its own node builder
            return ((FXIconPaneBuilder) v).newIconPane();
        }

        Function<Object,Node> override = ide.nodeBuilders.get(v.getClass());
        if (override != null) {
            //create via the type-dependent override
            return override.apply(v);
        } else {
            //create the default:
            BorderPane bp = new BorderPane();

            Label label = new Label(k);

            label.setWrapText(true);
            label.setTextOverrun(OverrunStyle.ELLIPSIS);

            label.getStyleClass().add("h1");

            label.setCache(true);

            bp.setCenter(label);

            label.setTooltip(new Tooltip(v.toString()));

            return bp;
        }
    }


//    public class PluginPane extends JPanel {
//        private final OperatorRegistration plugin;
//
//        public PluginPane(OperatorRegistration p) {
//            super(new BorderLayout());
//
//            this.plugin = p;
//            final JLabel j = new JLabel(p.IOperator.toString());
//            j.setFont(Video.monofont);
//            add(j, BorderLayout.NORTH);
//
//            JPanel buttons = new JPanel(new FlowLayout());
//            add(buttons, BorderLayout.EAST);
//
//            JCheckBox e = new JCheckBox();
//            e.setSelected(p.isEnabled());
//            e.addActionListener(new ActionListener() {
//                @Override public void actionPerformed(ActionEvent ae) {
//                    SwingUtilities.invokeLater(new Runnable() {
//                        @Override public void run() {
//                            boolean s = e.isSelected();
//                            p.setEnabled(s);
//                        }
//                    });
//                }
//            });
//            buttons.add(e);
//
//            JButton removeButton = new JButton("X");
//            removeButton.addActionListener(new ActionListener() {
//                @Override public void actionPerformed(ActionEvent ae) {
//                    SwingUtilities.invokeLater(new Runnable() {
//                        @Override public void run() {
//                            removePlugin(plugin);
//                        }
//                    });
//                }
//            });
//
//            buttons.add(removeButton);
//
//
//            add(new ReflectPanel(p.IOperator), BorderLayout.CENTER);
//        }
//
//    }
//
//
//
//    protected void update() {
//        content.removeAll();
//
//        int i = 0;
//        List<OperatorRegistration> ppp = nar.getPlugins();
//        if (!ppp.isEmpty()) {
//            for (OperatorRegistration p : ppp) {
//                PluginPane pp = new PluginPane(p);
//                pp.setBorder(new BevelBorder(BevelBorder.RAISED));
//                addVertically(pp, i++);
//            }
//        }
//        else {
//            addVertically(new JLabel("(No plugins active.)"), i++);
//        }
//
//
//        //contentWrap.doLayout();
//        //contentWrap.validate();
//    }
//
//
//    @Override
//    public void visibility(boolean appearedOrDisappeared) {
//        observer.setActive(appearedOrDisappeared);
//    }
//
//    private JMenuItem newAddPluginItem(Class c) {
//        String name = c.getSimpleName();
//        JMenuItem j = new JMenuItem(name);
//        j.addActionListener(new ActionListener() {
//            @Override public void actionPerformed(ActionEvent e) {
//                addPlugin(c);
//                SwingUtilities.invokeLater(new Runnable() {
//                    @Override public void run() {
//                        update();
//                    }
//                });
//            }
//        });
//        return j;
//    }
//
//    protected void addPlugin(Class c) {
//        try {
//            IOperator p = (IOperator)c.newInstance();
//            nar.on(p);
//        } catch (Exception ex) {
//            JOptionPane.showMessageDialog(this, ex.toString());
//        }
//    }
//    protected void removePlugin(OperatorRegistration ps) {
//        ps.off();
//    }
//
//


}

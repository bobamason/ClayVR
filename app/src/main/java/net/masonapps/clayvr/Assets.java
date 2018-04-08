package net.masonapps.clayvr;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Bob on 8/29/2017.
 */

public class Assets {
    public static final String FOLDER_MODELS = "models/";
    public static final String FOLDER_RAW = "raw/";
    public static final String ROOM_MODEL = Assets.FOLDER_MODELS + "room_textured.g3db";
    public static final String FOLDER_AUDIO = "audio/";
    public static final String HUMAN_TEMPLATE = FOLDER_MODELS + "human_template.sculpt";
    public static final String HUMAN_TEMPLATE_BVH = FOLDER_MODELS + "human_template.bvh";
    public static final String ICOSPHERE_MESH_MED = FOLDER_MODELS + "icosphere_med.sculpt";
    public static final String ICOSPHERE_MED_BVH = FOLDER_MODELS + "icosphere_med.bvh";
    public static final String ICOSPHERE_MESH_HI = FOLDER_MODELS + "icosphere_hi.sculpt";
    public static final String ICOSPHERE_PLY = FOLDER_MODELS + "icosphere.ply";
    public static final String BUTTON_SOUND_FILE = FOLDER_AUDIO + "fins_menu_button.wav";
    public static final String LOADING_SPINNER_ASSET = FOLDER_RAW + "loading_spinner.png";
    public static final String SKY_TEXTURE = FOLDER_RAW + "skydome_1.png";
    public static List<String> modelList = Arrays.asList(ICOSPHERE_MESH_MED, ICOSPHERE_MESH_HI);
}

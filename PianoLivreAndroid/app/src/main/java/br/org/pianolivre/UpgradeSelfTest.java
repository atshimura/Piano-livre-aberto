package br.org.pianolivre;
import android.content.*;import java.io.*;import java.util.*;
/** Read-only checks of synthetic preferences seeded by the emulator workflow. */
final class UpgradeSelfTest {
 static int run(Context context,SharedPreferences prefs)throws Exception{
  if(!prefs.getBoolean("upgrade_test_fixture",false))throw new AssertionError("Upgrade fixture missing");
  List<LocalProfiles.Profile> profiles=new LocalProfiles(prefs).list();
  if(profiles.size()!=2||profiles.get(1).id!=7||profiles.get(1).avatar!=3||prefs.getInt("active_profile",-1)!=7)throw new AssertionError("Upgrade profiles");
  if(prefs.getInt("user_7_mode",-1)!=1||prefs.getInt("user_7_hand",-1)!=2||!prefs.getBoolean("user_7_keyboard",false))throw new AssertionError("Upgrade settings");
  if(prefs.getLong("user_7_position",0)!=Double.doubleToLongBits(.75)||prefs.getLong("user_7_tempo",0)!=Double.doubleToLongBits(.8))throw new AssertionError("Upgrade session position/speed");
  if(!new LibraryStore(prefs,7).snapshot().favorites.contains("imported/upgrade.mxl"))throw new AssertionError("Upgrade favorite");
  org.json.JSONObject history=new StudyHistory(prefs,7).snapshot();
  if(history.getJSONArray("records").length()<1||history.getJSONArray("records").getJSONObject(0).getLong("elapsedMs")!=60000||history.getJSONObject("pieces").getJSONObject("imported/upgrade.mxl").getInt("goalMinutes")!=15)throw new AssertionError("Upgrade synthetic session history");
  String path=SessionFiles.resolve(context.getFilesDir(),prefs.getString("user_7_file",""));
  byte[] original;try(InputStream in=context.getAssets().open("scores/ode-to-joy-ludwig-van-beethoven.mxl")){original=Score.readLimited(in,16*1024*1024);}
  byte[] restored;try(InputStream in=new FileInputStream(path)){restored=Score.readLimited(in,16*1024*1024);}
  if(!Arrays.equals(original,restored))throw new AssertionError("Upgrade source bytes");
  if(Score.read(new ByteArrayInputStream(restored),path).events.isEmpty())throw new AssertionError("Upgrade score parse");
  return 7;
 }
}

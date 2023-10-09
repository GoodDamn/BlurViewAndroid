package good.damn.first

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.pm.ConfigurationInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import good.damn.shaderblur.views.BlurShaderView
import java.util.Random

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity";
    private lateinit var mSurfaceBlurView: BlurShaderView;

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        val rootLayout = findViewById<FrameLayout>(R.id.mainActivity_rootFrameLayout);
        val scrollView = rootLayout.getChildAt(0) as ScrollView;

        val contentLayout = scrollView.getChildAt(0) as LinearLayout;

        val random = Random();

        mSurfaceBlurView = BlurShaderView(this)
        mSurfaceBlurView.setSourceView(scrollView)

        val configurationInfo: ConfigurationInfo =
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo;

        Toast.makeText(
            this,
            configurationInfo.glEsVersion,
            Toast.LENGTH_LONG)
            .show();

        for (i in 0..10) {
            val button = Button(this);
            val textView = TextView(this);
            textView.text = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.\n" +
                    "\n" +
                    "Why do we use it?\n" +
                    "It is a long established fact that a reader will be distracted by the readable content of a page when looking at its layout. The point of using Lorem Ipsum is that it has a more-or-less normal distribution of letters, as opposed to using 'Content here, content here', making it look like readable English. Many desktop publishing packages and web page editors now use Lorem Ipsum as their default model text, and a search for 'lorem ipsum' will uncover many web sites still in their infancy. Various versions have evolved over the years, sometimes by accident, sometimes on purpose (injected humour and the like).\n" +
                    "\n" +
                    "\n" +
                    "Where does it come from?\n" +
                    "Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of \"de Finibus Bonorum et Malorum\" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, \"Lorem ipsum dolor sit amet..\", comes from a line in section 1.10.32.\n" +
                    "\n" +
                    "The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from \"de Finibus Bonorum et Malorum\" by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.";
            button.text = (i+i+i).toString();
            button.setBackgroundColor(Color.argb(random.nextInt(100)+155,
                random.nextInt(100)+155,
              random.nextInt(100)+155,
               random.nextInt(100)+155));

            textView.setBackgroundColor(Color.argb(random.nextInt(100)+155,
                random.nextInt(100)+155,
                random.nextInt(100)+155,
                random.nextInt(100)+155));

            contentLayout.addView(textView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            contentLayout.addView(button,FrameLayout.LayoutParams.MATCH_PARENT, 180);
        }

        Handler(Looper.getMainLooper()).postDelayed({
            rootLayout.addView(mSurfaceBlurView, FrameLayout.LayoutParams.MATCH_PARENT, 150);
        },1500);

    }

    override fun onDestroy() {
        super.onDestroy()
        mSurfaceBlurView.clean()
    }

    override fun onResume() {
        mSurfaceBlurView.onResume()
        super.onResume()
    }

    override fun onPause() {
        mSurfaceBlurView.onPause()
        super.onPause()
    }

}
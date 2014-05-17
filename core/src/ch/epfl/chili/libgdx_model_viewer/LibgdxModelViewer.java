package ch.epfl.chili.libgdx_model_viewer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationDesc;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController.AnimationListener;
import com.badlogic.gdx.math.Vector3;

public class LibgdxModelViewer extends ApplicationAdapter implements AnimationListener{
	
	/** Environment that describes the lights etc. */
	private Environment environment;
	
	/** GL camera */
	private PerspectiveCamera camera;
	
	/** Gesture processor that moves the camera */
	private CameraInputController camController;
	
	/** Model batch that renders our models */
	private ModelBatch modelBatch;
	
	/** Sprite batch that renders our 2D objects */
	private SpriteBatch spriteBatch;
	
	/** Font that we use to render text */
	private BitmapFont font;
	
	/** The model to render */
	private Model model;
	
	/** Instance of our model */
	private ModelInstance instance;
	
	/** X axis arrow instance */
	private ModelInstance xi;
	
	/** Y axis arrow instance */
	private ModelInstance yi;
	
	/** Z axis arrow instance */
	private ModelInstance zi;
	
	/** Animation controller */
	private AnimationController animationController;
	
	/** Scene light */
	private DirectionalLight light;
	
	/** Loader of our model */
	private AssetManager assetManager;
	
	/** Whether our model is still loading */
	private boolean loading = true;
	
	/** Model filename */
	private String modelName = "funky_palm_tree/funky_palm_tree.g3db";
	
	/** Index of the current animation */
	private int currentAnimation = -1;
	
	@Override
	public void create(){
		
		//Load our model
		assetManager = new AssetManager();
		assetManager.load(modelName, Model.class);
		
		//Create our environment
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		light = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
		environment.add(light);

		//Initialize batches and fonts
		modelBatch = new ModelBatch();
		spriteBatch = new SpriteBatch();
		font = new BitmapFont();

		//Initialize the camera
		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(10f, 10f, 10f);
		camera.lookAt(0,0,0);
		camera.near = 1f;
		camera.far = 300f;
		camera.update();

		//Create X,Y,Z frame arrows 
		ModelBuilder modelBuilder = new ModelBuilder();
		Model x = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(10,0,0),
				new Material(ColorAttribute.createDiffuse(Color.RED)),
				Usage.Position | Usage.Normal);
		xi = new ModelInstance(x);

		Model y = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,10,0),
				new Material(ColorAttribute.createDiffuse(Color.GREEN)),
				Usage.Position | Usage.Normal);
		yi = new ModelInstance(y);
		
		Model z = modelBuilder.createArrow(new Vector3(0,0,0), new Vector3(0,0,10),
				new Material(ColorAttribute.createDiffuse(Color.BLUE)),
				Usage.Position | Usage.Normal);
		zi = new ModelInstance(z);
		
		//Initialize the gesture processor
		camController = new CameraInputController(camera);
		Gdx.input.setInputProcessor(camController);
	}

	/**
	 * Gets called when model is loaded.
	 */
	private void doneLoading() {
		
		//Get our model and its animations if any
        model = assetManager.get(modelName, Model.class);
        instance = new ModelInstance(model); 
        if(instance.animations.size > 0){
        	animationController = new AnimationController(instance);
        	animationController.allowSameAnimation = true;
        	currentAnimation = 0;
        	animationController.animate(instance.animations.get(currentAnimation).id, this, 0.5f);
        }
        loading = false;
    }
	
	@Override
	public void render () {
		
		//Continue loading our model
		if (loading && assetManager.update())
            doneLoading();
		
		//Process our input
		camController.update();
		
		//Project light towards the object from our viewpoint
		light.direction.set(camera.direction);

		//Update animation
		if(animationController != null)
			animationController.update(Gdx.graphics.getDeltaTime());
		
		//Draw 3D objects
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		modelBatch.begin(camera);
		if(instance != null)
			modelBatch.render(instance, environment);
		modelBatch.render(xi,environment);
		modelBatch.render(yi,environment);
		modelBatch.render(zi,environment);
		modelBatch.end();
		
		//Write animation info
		if(currentAnimation >= 0){
			spriteBatch.begin();
			font.draw(spriteBatch, "Current animation (" + (currentAnimation + 1) + " of " + instance.animations.size + "): " 
			+ instance.animations.get(currentAnimation).id, 10, Gdx.graphics.getHeight() - 10);
			spriteBatch.end();
		}
	}

	@Override
	public void dispose(){
		modelBatch.dispose();
		model.dispose();
	}

	@Override
	public void resize(int width, int height){}

	@Override
	public void pause(){}

	@Override
	public void resume(){}

	@Override
	public void onEnd(AnimationDesc animation){
		
		//Blend to the next animation
		currentAnimation = (currentAnimation + 1)%instance.animations.size;
		animationController.animate(instance.animations.get(currentAnimation).id, this, 0.5f);
	}

	@Override
	public void onLoop(AnimationDesc animation){}
}

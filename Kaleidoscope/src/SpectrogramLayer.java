import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;
import processing.core.PApplet;
import processing.core.PImage;


class SpectrogramLayer extends CircularLayer implements AudioProcessor
{

	private AudioDispatcher audioDispatcher;
	private final FFT fft;
	private final float[] amplitudes, transformbuffer;
	private final int samplesPerSegment;

	public SpectrogramLayer(PApplet parent, PImage img, int segmentCount, int innerRadius, int outerRadius, AudioDispatcher audioDispatcher)
	{
		super(parent, img, segmentCount, innerRadius, outerRadius);

		this.audioDispatcher = audioDispatcher;
		amplitudes = new float[Kaleidoscope.audioBufferSize / 2];
		transformbuffer = new float[Kaleidoscope.audioBufferSize];
		samplesPerSegment = amplitudes.length / segmentCount;
		assert samplesPerSegment > 0;
	  fft = new FFT(Kaleidoscope.audioBufferSize);
	  audioDispatcher.addAudioProcessor(this);
	}

	public void run()
	{
	  parent.pushMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
		parent.translate(parent.width/2, parent.height/2); // translate to the right-center
		parent.stroke(255);
		parent.strokeWeight(0.5f);
		parent.noStroke();
		parent.beginShape(PApplet.TRIANGLE_STRIP); // input the shapeMode in the beginShape() call
		parent.texture(currentImage); // set the texture to use

	  for (int i = 0; i < segmentCount + 1; i++)
	  {
	    int imi = i % segmentCount; // make sure the end equals the start

	    float dynamicOuter = getAvg(imi);
	    //println(dynamicOuter);

	    drawCircleVertex(imi, innerRadius); // draw the vertex using the custom drawVertex() method
	    drawCircleVertex(imi, outerRadius * (dynamicOuter + 1)); // draw the vertex using the custom drawVertex() method
	  }

		parent.endShape(); // finalize the Shape
		parent.popMatrix(); // use push/popMatrix so each Shape's translation does not affect other drawings
	}

	private float getAvg(int i)
	{
		final int offset = i * samplesPerSegment;
		float sum = amplitudes[offset];
    for (int j = 1; j < samplesPerSegment; j++)
    	sum += amplitudes[offset + j];
    return sum / samplesPerSegment;
	}

	public boolean process( AudioEvent audioEvent )
	{
		float[] audioFloatBuffer = audioEvent.getFloatBuffer();
		System.arraycopy(audioFloatBuffer, 0, transformbuffer, 0, audioFloatBuffer.length);
		fft.forwardTransform(transformbuffer);
		fft.modulus(transformbuffer, amplitudes);
		return true;
	}

	public void processingFinished()
	{
		// Nothing to do here
	}

}

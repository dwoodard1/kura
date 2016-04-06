var ethWires = (function() {
	var client = {};
	var clientConfig = {};

	client.render = function(obj) {
		console.log("Rendering JointJs...");
		clientConfig = obj;
		console.log(clientConfig);
		//doSetup();
		doJointJs();
		//top.jsniUpdateWireConfig("hello");
	};




	function doSetup() {
		//$("#wires-modal-body").load("kura_wires.html", doJointJs);
	}

	function addComponent() {

	}

   function doJointJs() {
	   var graph = new joint.dia.Graph;

       var paper = new joint.dia.Paper({
           el: $('#wires-graph'),
           width: 600,
           height: 200,
           model: graph,
           gridSize: 1
       });

       var rect = new joint.shapes.basic.Rect({
           position: { x: 100, y: 30 },
           size: { width: 50, height: 30 },
           attrs: { rect: { fill: 'blue' }, text: { text: 'comp1', fill: 'white' } }
       });

       var rect2 = new joint.shapes.basic.Rect({
           position: { x: 170, y: 30 },
           size: { width: 50, height: 30 },
           attrs: { rect: { fill: 'red' }, text: { text: 'comp2', fill: 'white' } }
	   });

	   var rect3 = new joint.shapes.basic.Rect({
           position: { x: 100, y: 30 },
           size: { width: 50, height: 30 },
           attrs: { rect: { fill: 'blue' }, text: { text: 'comp1', fill: 'white' } }
       });

	   var rect4 = rect3.clone();
	   rect4.translate(200);

       var link = new joint.dia.Link({
           source: { id: rect3.id },
           target: { id: rect4.id }
       });

	   graph.addCells([rect3, rect4, link])
       graph.on('all', function(eventName, cell) {
           console.log(arguments);
       });
   }

   return client;

}());

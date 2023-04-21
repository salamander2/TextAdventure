package adventureGame;

//import java.util.ArrayList;
import java.util.HashMap;

class Item {


	/* Handle Item Synonyms
	 * This method replaces synonyms of items with the correct item name.
	 * It's called right after the parse string function.
	 * 
	 * We're not going to solve this with the hashmap :
	 * ... even though it would probably work to have the same item added under two different keys
	 *     we would then be able to add both the torch and the flashlight to your inventory
	 *     even though the are the same item.
	 */
	static String itemSynonyms(String text) {
		String[][] synonyms = {
				//correct word, synonym
				{"lake", "water"},
				{"batteries", "battery"},
				{"flashlight", "torch"},
				{"murals", "mural"},
		};
		for (int i = 0; i < synonyms.length; i++) {
			//					    original (synonym),		replacement (correct)
			text = text.replaceAll(	synonyms[i][1],			synonyms[i][0]);
		}
		return text; 
	}


	String descrRoom = ""; //description of item to meld with room description. 
	//if this is empty. then the item does not show up in the list of items in the room description (perhaps because it is already there - eg. black lake)	
	String descrLook = ""; //description of item when you look at it.

	String descrRead = "";	//what is displayed when you read the item. If empty, then there is nothing that you can read on it.
	
	private int foodpoints = 0;		//how many food points the food has (0=not edible)
	boolean regenFood = false; 		//only the mangosteen bush has infinite food.

	/* tool related properties */
	private boolean isActivated = false;
	String descrActive = ""; //the "description" changes to this when it is activated
	String activatedMethod = ""; //method to be run upon activation. begins with "a_"

	/* moving related properties */
	boolean isCarryable = true;
	String moveDesc = "";   //text to be displayed when item is moved
	String moveMethod = ""; //method to be run upon moving. begins with "m_"
	String revealItem = ""; //something is revealed when it is moved.	

	/* container related properties */
	boolean isContainer = false;
	String itemContained = "";  //NOTE: containers can only contain 1 item!
	String openMethod = "";    //method to be run upon opening
	boolean isOpen = false;    //used for containers (and windows)
	String openRequires = "";  //object needed in order to open this item

	//basic constructor
	Item(String descr) {
		this.descrLook = descr;		
	}

	/* all getter and setter methods here */
	int getFoodPoints() {
		return this.foodpoints;
	}

	void setActivate(boolean b) {
		this.isActivated = b;
	}

	boolean isActivated() {
		return this.isActivated;
	}

	/***************************************************************************************
	 * Make each item in the game and add it to the list.
	 * Also place the item in the specific room once rooms are made
	 * (That way a room never contains an object that is not in this list.)

	 * This static method obviates having subclasses of items for Container etc.
	 * since all objects added are items. <-- what???
	 */

	static void setUpItems(HashMap<String,Item> itemList, HashMap<String,Room> roomList) {
		Item z = new Item("A decent piece of kudu biltong");
		z.descrRoom = "You spot something that looks like biltong.";
		z.foodpoints = 11;
		itemList.put("biltong",z);
		roomList.get("path1").items.add("biltong");

		z = new Item("a sharp knife with a bone handle");
		z.descrRoom = "There is a knife embedded in the tree trunk.";
		z.descrLook = "It looks like a throwing knife!";
		itemList.put("knife",z);
		roomList.get("tree1").items.add("knife");

		z = new Item("A carefully folded piece of paper with writing on it.");
		z.descrRoom= "Some pieces of paper have blown under a bush.";
		z.descrRead = "To return to your world, you need to ring the silver bell in the special location."
				+ "\n <--- CUT HERE --->";
		itemList.put("paper",z);
		roomList.get("maze2").items.add("paper");
		
		z = new Item("a small yellow paper with writing on it");
		z.descrRoom = "There's a yellowpaper stuck on a thorn bush";
		z.descrRead = "Drop items to map the maze you're in.";
		itemList.put("yellowpaper",z);
		
		z = new Item("a small pink paper with writing on it");
		z.descrRoom = "There's a pinkpaper stuck on a thorn bush";
		z.descrRead = "The bell needs an emerald.";
		itemList.put("pinkpaper",z);
		
		z = new Item("a small blue paper with writing on it");
		z.descrRoom = "There's a bluepaper stuck on a thorn bush";
		z.descrRead = "The emerald is in a rock.";
		itemList.put("bluepaper",z);

		z = new Item("a pile of dusty leaves");
		z.descrRoom = "Piles of deciduous leaves lie on the ground.";
		z.isCarryable = false;
		z.moveDesc = "You kick the leaves around seriously hurting your foot and raising some dust."
				+ " \nAfter you finish coughing, you see a hammer in the leaves.";
		z.revealItem = "hammer";
		z.moveMethod = "m_moveLeaves"; //use this to reduce health.
		itemList.put("leaves", z);
		roomList.get("forest2").items.add("leaves");

		z = new Item("A hammer from Canadian Tire! It has a wooden handle.");
		z.descrRoom = "A hammer lies on the ground.";
		z.activatedMethod = "a_useHammer";
		itemList.put("hammer", z);

		z = new Item("boring large rocks");
		z.descrRoom = "There are some loose rocks here.";
		z.isCarryable = false;
		itemList.put("rock",z);
		roomList.get("cave1").items.add("rock");
		roomList.get("cave2").items.add("rock");
		//		or should this be called rocks?		

		//		THIS DOES NOT WORK!
		//		it cannot be called "rock2" because who will type that?
		//		There must be a special method that check to see if the rock is in room X. If it is, then treat it as rock2		
		z = new Item("This rock seems hollow. Hmmm... is it even a real rock? If only you had a hammer.");
		z.descrRoom = "There are some loose rocks here.";  
		z.isCarryable = false;
		z.isActivated = false;
		z.activatedMethod = "a_smashRock";
		itemList.put("\"rock\"",z);
		roomList.get("black_lake").items.add("\"rock\"");				

		z = new Item("a very useful flashlight: waterproof, knurled aluminum");
		//z.descrLook = "It won't work without batteries!";
		z.descrActive = "The flashlight is glowing brightly";
		z.descrRoom = "Someone dropped a flashlight at the side of the path.";
		itemList.put("flashlight",z);
		roomList.get("clearing").items.add("flashlight");

		z = new Item("strange flashlight battery");
		z.descrLook = "Whoa! This is a nuclear tri-lithium battery which will last 10 millennia!";
		z.descrRoom = "Is that a battery under a bush?";
		itemList.put("batteries", z);
		roomList.get("maze3").items.add("batteries");

		z = new Item("a securely locked heavy metal chest");
		z.descrRoom = "A heavy chest sits on a newly revealed shelf.";
		z.isCarryable = false;
		z.isContainer = true;
		z.isOpen = false;
		z.itemContained = "emerald";
		z.openRequires = "key";
		z.openMethod = "";
		itemList.put("chest",z);
		//add to a room

		z = new Item("It looks elven. Someone from Middle Earth was here.");
		z.descrRoom = "A dirty package wrapped in leaves is wedged under the door.";
		z.isContainer = true;
		z.itemContained = "lembas";
		itemList.put("package",z);

		z = new Item("A few precious wafers of lembas.");
		z.foodpoints = 21;
		z.descrRoom = "A package of lembas. Someone from Middle Earth was here.";
		itemList.put("lembas",z);

		z = new Item("You can't reach the glowing/shiny crystals, and,\n"
				+ " unfortuntately, you couldn't do anything with them even if you could.");
		z.isCarryable = false;
		itemList.put("crystals",z);
		roomList.get("treasury").items.add("crystals");
		roomList.get("cave2").items.add("crystals");

		z = new Item("The crystal is about 6 inches long and glows green or yellow "
				+ "with a brightness that responds to your mental command to it!.");
		z.descrRoom = ("One of the crystals from the cave has has broken off the wall and is glowing.");
		itemList.put("crystal",z);

		z = new Item("The lake is black and wet"); 
		z.isCarryable = false;
		z.descrRoom = "";		
		z.activatedMethod="a_getFromLake";
		itemList.put("lake",z);
		//itemList.put("water",z); //as long as there is no other water in the game
		roomList.get("black_lake").items.add("lake");

		//make the key!
		z = new Item("The silver key has a impatient multidimensional appearance.");
		z.descrRoom = "";
		itemList.put("key",z);
		roomList.get("black_lake").items.add("key");


		z = new Item("a heavy metal door with debris behind it");
		z.descrRoom = "The heavy door is blocking most of the western exit, but you can still squeeze past.";
		z.isCarryable = false;
		z.moveMethod = "m_moveTreasureDoor";
		z.moveDesc = "With great effort you move the door a few inches.\n "
				+ "You see a small package under the door.";
		z.revealItem = "package";
		itemList.put("door",z);
		roomList.get("tunnel3").items.add("door");
		roomList.get("treasury").items.add("door");

		z = new Item("A beautiful teardrop shaped emerald!");
		z.descrRoom = "You see a green gemstone.";		
		itemList.put("emerald",z); //this is in the chest 

		z = new Item("This lever has never been used. It gives off a menacing air.");
		z.descrRoom = "A black metal lever on the wall has a plaque under it.";
		z.isCarryable = false;
		z.moveDesc = "Feeling foolhardy, you yank the lever down.";
		z.moveMethod = "m_moveLever";
		z.revealItem = "chest";
		itemList.put("lever", z);
		roomList.get("secret_room").items.add("lever");

		z = new Item("The plaque says: \"Warning: Earthquake Generator\n"
				+ "\t\tAuthorized use only (for terraforming)\"");
		z.isCarryable = false;
		z.descrRead = z.descrLook;
		itemList.put("plaque", z);
		roomList.get("secret_room").items.add("plaque");

		z = new Item("A silver bell. It is missing a jeweler clapper.");
		z.descrActive = "A silver bell with an emerald clapper.";
		z.descrRoom = "A silver bell lies on the floor."; 
		itemList.put("bell", z);

		z = new Item("The wall is has messages (graffiti?) carvared into it. You can decipher the meaning!");
		z.isCarryable = false;
		z.descrRoom = "The west wall is covered with some sort of writing";
		z.descrRead = "\n<><><><><><><><>\n"
				+ "<>Sember ubi sububi\n"
				+ "<>Pippin waz hyre\n"
				+ "<>Lake67d43f53down86k36goodwater67u3KEY\n"
				+ "<>Pd^42^4t^j secret room a32@ds@wd@f tunnel\n"
				+ "<>Lembas cures poison.\n"
				+ "<><><><><><><><>\n";
		itemList.put("wall", z);
		roomList.get("cave3").items.add("wall");

		z = new Item("The murals are fascinating, but your brain cannot comprehend what they are intended to depict.");
		z.isCarryable = false;
		itemList.put("mural", z);
		roomList.get("secret_room").items.add("mural");

		/*
		z = new Item("The basket contains a serving of khao niao.");
		z.foodpoints = 11;
		z.isCarryable = false;
		z.descrRoom = "You notice a small bamboo basket emitting a lovely aroma.";
		itemList.put("khao niao",z);
		itemList.put("basket",z);
		roomList.get("forest2").items.add("khao niao");
		roomList.get("forest2").items.add("basket");
		 */
		
		z = new Item("Hey it's a mangosteen!");
		z.foodpoints = 6;
		z.regenFood = true;
		z.descrRoom = "You notice a bush full of purple fruit.";
		itemList.put("fruit",z);
		roomList.get("maze7").items.add("fruit");		

		z = new Item("I do believe that it's kimchi");
		z.foodpoints = 16;
		z.descrRoom = "You notice a something smelly in the bushes.\n"
				+ "Could it be a type of Korean food?";
		itemList.put("kimchi",z);
		roomList.get("forest2").items.add("kimchi");		

	}

}

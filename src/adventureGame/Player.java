package adventureGame;

public class Player {
	
	/* Variables:
	 * foodPoints: 100=too full to eat
	 * 				0 - 20 = hungry
	 * 				-15 - 0 = starving
	 * 				< -15: you starve to death. 
	 * health: max = 100;
	 * 		0 = you die
	 * status: healthy, asleep, broken leg, sick, poisoned, ...
	 */
	
	final String name;
	int turns = 0;
	private int foodPoints = 50;
	private int health = 70;
	boolean isThirsty = false;
	private String status = "";
	
	Player(String name) {
		this.name = name;
	}
	
	void injury(int h) {
		health -= h;
	}
	
	int getHealth() { return health; }
	int getFoodPoints() { return foodPoints; }
	void hunger(int n) {
		if (n > 0&& n < 100) foodPoints -= n;		
	}
	void heal(int h) {
		if (h > 0 && h < 100) health += h;
	}
	
	boolean eat(Item q) {
		if (this.foodPoints >= 100) {
			System.out.println("You are too full to eat more.");
			return false;
		}
		if (q.getFoodPoints() <= 0) {
			System.out.println("You can't eat that!");
			return false;
		}
		this.foodPoints += q.getFoodPoints();
		System.out.println("Mmmm... yummy.");
		return true;
	}
}

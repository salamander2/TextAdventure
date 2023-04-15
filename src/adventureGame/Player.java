package adventureGame;

public class Player {

	/* Variables:
	 * foodPoints: <= 0: you starve to death. 
	 * health: max = 100;
	 * 		0 = you die
	 * status: healthy, asleep, broken leg, sick, poisoned, ...
	 */
	private static final int STARVING = 15;
	private static final int HUNGRY = 35;
	private static final int TOOFULL = 70;

	//private? getter & setter?
	final String name;
	int turns = 0;
	private int foodPoints = 50;
	private int health = 70;
	boolean isThirsty = false;
	boolean isPoisoned = false;
	//private String status = "";

	Player(String name) {
		this.name = name;
	}

	int getHealth() { return health; }
	int getFoodPoints() { return foodPoints; }
	void hunger(int n) {
		if (n > 0&& n < 100) foodPoints -= n;		
	}
	void injury(int h) {
		health -= h;
	}
	void heal(int h) {
		if (h > 0 && h < 100) health += h;
	}

	boolean eat(String name, Item q) {
		if (this.foodPoints >= TOOFULL) {
			System.out.println("You are too full to eat more.");
			return false;
		}
		if (q.getFoodPoints() <= 0) {
			System.out.println("You can't eat that!");
			return false;
		}
		this.foodPoints += q.getFoodPoints();
		if (name.equals("lembas")) {
			if (isPoisoned) {
				isPoisoned = false;
				System.out.println("You are cured from your poison.");
			}
			this.health += 12;
			if (foodPoints <= STARVING) foodPoints = STARVING +6;
			System.out.println("You feel healthier and satisfied.");
			return true;
		}
		System.out.println("Mmmm... yummy.");
		return true;
	}

	void update() {
		turns++;
		foodPoints --;
		
		if (isPoisoned) health -= 8;
		if (foodPoints < STARVING) health -= 3;
	}
	
	boolean checkup() {
		
		if (isPoisoned) {
			System.out.println("You are severly poisoned.");
		}
		
		if (health < 0 && foodPoints > STARVING) {
			System.out.println("You died of a bad infection. How sad.");
			return false;
		}
		
		if (foodPoints < STARVING) {
			System.out.println("You are starving.");
		} else if (foodPoints < HUNGRY) {
			System.out.println("You are getting hungry.");
		}
		
		if (foodPoints <= 0 || health <= 0 && foodPoints < STARVING) {
			
			//DEBUG
			System.out.println("food=" + foodPoints);
			System.out.println("health=" + health);
			System.out.println("You starved to death. How sad.");
			return false;
		}
		return true;
	}
}

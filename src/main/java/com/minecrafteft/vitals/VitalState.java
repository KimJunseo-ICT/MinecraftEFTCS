package com.minecrafteft.vitals;

/** Authoritative survival values. No Minecraft health/food values are stored here. */
public final class VitalState {
    public static final double MAX_HEALTH = 100;
    public static final double MAX_RESOURCE = 100;
    public static final double MAX_STAMINA = 150;
    public static final double RED_STAMINA = MAX_STAMINA*.15;
    private double health = MAX_HEALTH, energy = 100, hydration = 100, stamina = MAX_STAMINA;
    private boolean sprintLocked, wasSprinting;
    private double deprivationElapsed;
    private double staminaRecoveryDelay;
    private boolean sprintRestartRequired,spentStamina;
    private int sprintRestartAcknowledgement;
    public int sprintRestartAcknowledgement() { return sprintRestartAcknowledgement; }
    public void acknowledgeSprintRestart(int request) { sprintRestartAcknowledgement=request; }

    public double health() { return health; }
    public double energy() { return energy; }
    public double hydration() { return hydration; }
    public double stamina() { return stamina; }
    public boolean sprintLocked() { return sprintLocked; }
    public boolean sprintRestartRequired() { return sprintRestartRequired; }
    public boolean spentStamina() { return spentStamina; }
    public double staminaRecoveryDelay() { return staminaRecoveryDelay; }
    public void restoreSprintRuntime(double delay,boolean restart) {
        staminaRecoveryDelay=clamp(delay,.5);sprintRestartRequired=restart;
    }
    public boolean restartSprint() {
        if(!sprintRestartRequired || sprintLocked || stamina<=RED_STAMINA) return false;
        sprintRestartRequired=false;return true;
    }
    public double deprivationElapsed() { return deprivationElapsed; }
    public void restoreDeprivationElapsed(double seconds) { deprivationElapsed = nonnegative(seconds); }
    public static double savedHealth(double health, double savedMaximum) {
        double maximum = Double.isFinite(savedMaximum) && savedMaximum > 0 ? savedMaximum : MAX_HEALTH;
        return clamp(health / maximum * MAX_HEALTH, MAX_HEALTH);
    }
    public static double savedStamina(double stamina,double savedMaximum) {
        double maximum=Double.isFinite(savedMaximum) && savedMaximum>0?savedMaximum:100;
        return clamp(stamina/maximum*MAX_STAMINA,MAX_STAMINA);
    }
    public void damage(double amount) { health = clamp(health - nonnegative(amount), MAX_HEALTH); }
    public void heal(double amount) { if (health > 0) health = clamp(health + nonnegative(amount), MAX_HEALTH); }
    public void revive(double amount) { health = clamp(amount, MAX_HEALTH); }
    public void energy(double amount) { energy = clamp(energy + finite(amount), MAX_RESOURCE); }
    public void hydrate(double amount) { hydration = clamp(hydration + finite(amount), MAX_RESOURCE); }

    public void update(double health, double energy, double hydration, double stamina, boolean locked) {
        update(health,energy,hydration,stamina,locked,false);
    }
    public void update(double health, double energy, double hydration, double stamina, boolean locked,boolean restart) {
        this.health = clamp(health, MAX_HEALTH);
        this.energy = clamp(energy, MAX_RESOURCE);
        this.hydration = clamp(hydration, MAX_RESOURCE);
        this.stamina = clamp(stamina, MAX_STAMINA);
        sprintLocked = locked && this.stamina <= RED_STAMINA;
        wasSprinting = false;
        deprivationElapsed = 0;
        sprintRestartRequired=restart;spentStamina=false;
    }

    /** Seconds are simulation time; paused worlds and offline players never drain. */
    public void tick(double seconds, boolean sprinting, boolean moving, double energyPerMinute,
                     double hydrationPerMinute, double sprintDrain, double walkRecovery, double idleRecovery) {
        seconds = nonnegative(seconds);
        energy(-nonnegative(energyPerMinute) * seconds / 60);
        hydrate(-nonnegative(hydrationPerMinute) * seconds / 60);
        // Sprint flags represent start/stop intent. A missing movement packet or
        // a stationary tick must not end a continuing sprint when the bar turns red.
        if (stamina <= RED_STAMINA && ((!wasSprinting && sprinting) || (wasSprinting && !sprinting)))
            sprintLocked = true;
        boolean running = sprinting && !sprintLocked && !sprintRestartRequired;
        spentStamina=false;
        if (running && moving) {
            double before=stamina;
            stamina = clamp(stamina - nonnegative(sprintDrain) * seconds, MAX_STAMINA);
            spentStamina=stamina<before;
            if(spentStamina) staminaRecoveryDelay=.5;
            if (stamina == 0) { sprintLocked = true;sprintRestartRequired=true; }
        } else {
            double recoverySeconds=Math.max(0,seconds-staminaRecoveryDelay);
            staminaRecoveryDelay=Math.max(0,staminaRecoveryDelay-seconds);
            stamina = clamp(stamina + nonnegative(moving ? walkRecovery : idleRecovery) * recoverySeconds, MAX_STAMINA);
            if (stamina > RED_STAMINA) sprintLocked = false;
        }
        wasSprinting = running;
    }

    public double deprivationDamage(double energyDamage, double hydrationDamage) {
        return (energy == 0 ? nonnegative(energyDamage) : 0)
                + (hydration == 0 ? nonnegative(hydrationDamage) : 0);
    }
    /** Wait a full interval after depletion, then damage in spaced pulses. */
    public double tickDeprivation(double seconds, double interval, double energyDamage, double hydrationDamage) {
        double damage = deprivationDamage(energyDamage, hydrationDamage);
        if (damage == 0) { deprivationElapsed = 0; return 0; }
        double period = Math.max(.05, nonnegative(interval));
        deprivationElapsed += nonnegative(seconds);
        if (deprivationElapsed + 1e-9 < period) return 0;
        deprivationElapsed = Math.max(0, deprivationElapsed - period);
        return damage;
    }
    private static double finite(double number) { return Double.isFinite(number) ? number : 0; }
    private static double nonnegative(double number) { return Math.max(0, finite(number)); }
    private static double clamp(double number, double max) { return Math.max(0, Math.min(max, finite(number))); }
}

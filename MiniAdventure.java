package com.example;

import java.util.function.Supplier;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MiniAdventure extends Application {

    private VBox layout = null;

    public static class SceneNode {

        TextFlow text;
        Runnable onShow;
        Choice[] choices;
        SceneNode nextOnClick;

        public static class Choice {
            String text;
            SceneNode nextNode;
            Supplier<Boolean> condition;
            Runnable action;
            String tooltip;

            public Choice(String text, SceneNode nextNode) {
                this(text, nextNode, () -> true, null, null);
            }

            public Choice(String text, SceneNode nextNode, Supplier<Boolean> condition) {
                this(text, nextNode, condition, null, null);
            }

            public Choice(String text, SceneNode nextNode, Runnable action) {
                this(text, nextNode, null, action, null);
            }

            public Choice(String text, SceneNode nextNode, Supplier<Boolean> condition, Runnable action, String tooltip) {
                this.text = text;
                this.nextNode = nextNode;
                this.condition = condition;
                this.action = action;
                this.tooltip = tooltip;
            }
        }

        public SceneNode(String text) { this.text = makeTextFlow(text); }
        public SceneNode(Text text) { this.text = makeTextFlow(text); }
        public SceneNode(TextFlow text) { this.text = text; }
        public SceneNode(String text, Runnable onShow) { this.text = makeTextFlow(text); this.onShow = onShow; }
        public SceneNode(TextFlow text, Runnable onShow) { this.text = text; this.onShow = onShow; }
        public SceneNode(String text, Choice... choices) { this.text = makeTextFlow(text); this.choices = choices; }
        public SceneNode setChoices(Choice... choices) { this.choices = choices; return this; }
    }

    public class theFacts {
        public Boolean metCl = false;
        public Boolean metKing = false;
        public Boolean metJim = false;
        public Boolean spokeToChild = false;
        public Boolean spokeToCouple = false;
        public Boolean readNewspaper = false;
        public Character[] party;
        public ArrayList<Character> partyMembers = new ArrayList<>();
        int swapOutIndex = -1;
        private Runnable onPartyChange;

        SceneNode viewParty;
        SceneNode partySwapOutMenu;
        SceneNode partySwapInMenu;
        SceneNode backNode;

        public theFacts(Character[] party) {
            this.party = party;
        }

        void think() { // TODO : create thinking
            // display facts known and provide a button to transition to a node to 'consider' them
        }

        public void linkMenus(SceneNode viewParty, SceneNode partySwapOutMenu, SceneNode partySwapInMenu, SceneNode backNode) {
            this.viewParty = viewParty;
            this.partySwapOutMenu = partySwapOutMenu;
            this.partySwapInMenu = partySwapInMenu;
            this.backNode = backNode;

            viewParty.setChoices(
                new SceneNode.Choice("Adjust Party", partySwapOutMenu),
                new SceneNode.Choice("Back", backNode)
            );
            partySwapOutMenu.setChoices(getPartySwapOutChoices(viewParty, partySwapInMenu));
            partySwapInMenu.setChoices(getPartySwapInChoices(partySwapOutMenu, viewParty));
        }

        public void refreshViewParty(SceneNode backNode) {
            if (viewParty == null) return;

            TextFlow updatedInfo = partyInfo();

            viewParty.text = updatedInfo;
            linkMenus(viewParty, partySwapOutMenu, partySwapInMenu, backNode);
        }



        TextFlow partyInfo() {
            TextFlow infoTextFlow = makeTextFlow(bold("~~~ PARTY OVERVIEW ~~~\n\n"));
            for (Character c : party) {
                appendToTextFlow(infoTextFlow,
                        bold("--- " + c.getName() + " ---\n"),
                        italicize(c.description + "\nClass: " + c.className + "\nHealth: " + c.max_health +
                            "\nWeapon: " + c.weapon + " (Damage: " + c.damage + ")")
                );
                if (!c.getName().equals("Eli")) {
                    appendToTextFlow(infoTextFlow, "\nApproval: " + c.approvalScore);
                }
                appendToTextFlow(infoTextFlow, "\n\n");
            }
            return infoTextFlow;
        }

        SceneNode.Choice[] getPartySwapOutChoices(SceneNode backNode, SceneNode partySwapInNode) {
                ArrayList<SceneNode.Choice> list = new ArrayList<>();

            for (int i = 1; i < party.length; i++) {
                Character target = party[i];
                final int idx = i; // capture index for this choice
                // When chosen: record which slot to replace, then go to the swap-in menu
                list.add(new SceneNode.Choice(target.getName(), partySwapInNode, null, () -> {
                    swapOutIndex = idx;
                }, null));
            }
            // Back button
            list.add(new SceneNode.Choice("Back", backNode));

            return list.toArray(new SceneNode.Choice[0]);
        }

        SceneNode.Choice[] getPartySwapInChoices(SceneNode backNode, SceneNode afterActionNode) {
            ArrayList<Character> partyAsList = new ArrayList<>(Arrays.asList(party));
            ArrayList<SceneNode.Choice> list = new ArrayList<>();

            for (int i = 0; i < partyMembers.size(); i++) {
                Character candidate = partyMembers.get(i);
                if (!partyAsList.contains(candidate)) {
                    final Character cCopy = candidate;               // capture for lambda
                    list.add(new SceneNode.Choice(cCopy.getName(), afterActionNode, null, () -> {
                        // Do the swap using the class var set when swap-out was chosen
                        if (swapOutIndex >= 1 && swapOutIndex < party.length) {
                            addToParty(cCopy, swapOutIndex);
                        } else {
                            // defensive: log or handle unexpected -1
                            System.err.println("Invalid swapOutIndex: " + swapOutIndex);
                        }
                        swapOutIndex = -1; // clear after performing the swap
                    }, null));
                }
            }
            // Back button goes back to swap-out screen
            list.add(new SceneNode.Choice("Back", backNode));

            return list.toArray(new SceneNode.Choice[0]);
        }

        Character[] addToParty(Character newMember, int index) {
            party[index] = newMember;
            if (onPartyChange != null) onPartyChange.run(); // notify whoever is listening
            return party;
        }

        public void setOnPartyChange(Runnable callback) {
            this.onPartyChange = callback;
        }

        void addToPartyList(Character c) {
            partyMembers.add(c);
        }

        Character[] getParty() {
            return party;
        }

    }

    static class Character {
        String charName;
        int approvalScore;
        String className;
        String description;
        int max_health;
        int health;
        String weapon;
        int damage;
        Boolean isAlive = true;
        Boolean isDefending = false;
        Boolean isProtected = false;
        boolean attackUp = false;

        Map<String, Integer> abilityCooldowns = new HashMap<>();

        Character(String charName, String className, int max_health, String weapon, int weaponDamage, String description) {
            this.charName = charName;
            this.className = className;
            this.max_health = max_health;
            this.weapon = weapon;
            this.damage = weaponDamage;
            this.description = description;
            this.approvalScore = 0;
            health = max_health;

            if (charName.equals("Eli")) {
                abilityCooldowns.put("Protect", 0);
                abilityCooldowns.put("Static Shock", 0);
            }
            else if (charName.equals("Sea")) {
                abilityCooldowns.put("Attack Buff", 0);
            }
            else if (charName.equals("T")) {
                abilityCooldowns.put("Lullaby", 0);
            }
            else if (charName.equals("Ch")) {
                abilityCooldowns.put("Entangle", 0);
                abilityCooldowns.put("Tiny Heal", 0);
            }

        }

        boolean canUse(String abilityName) {
            return getCooldown(abilityName) == 0;
        }

        void setCooldown(String abilityName, int turns) {
            abilityCooldowns.put(abilityName, turns);
        }

        int getCooldown(String abilityName) {
            return abilityCooldowns.getOrDefault(abilityName, 0);
        }

        void reduceCooldowns() {
            for (String ability : abilityCooldowns.keySet()) {
                int cd = abilityCooldowns.get(ability);
                if (cd > 0) abilityCooldowns.put(ability, cd - 1);
            }
        }

        public void resetCooldowns() {
            abilityCooldowns.clear();
        }

        void editApproval(int amount) { approvalScore += amount; }
        int getApproval() { return approvalScore; }
        String getName() { return charName; }
        int getWeaponDamage() { return damage; }
        void takeDamage(int amount) { health -= amount; if (health <= 0) { health = 0; isAlive = false; } }
        boolean isDefending() { return isDefending; }
        void setDefending(Boolean defending) { isDefending = defending; }
        boolean isProtected() { return isProtected; }
        void setProtected(Boolean protecting) { isProtected = protecting; }
        boolean isAlive() { return isAlive; }
        void setAttackUp(Boolean yes) { attackUp = yes;}
        boolean isAttackUp() { return attackUp; }

        // For M One-shot Kill logic
        private boolean isLockedInAttack = false;

        public boolean isLockedInAttack() { return isLockedInAttack; }
        public void setLockedInAttack(boolean locked) { this.isLockedInAttack = locked; }

    }

    static class Enemy {
        String enemyName;
        int health;
        int max_health;
        int damage;
        boolean isAlive = true;
        boolean isStunned = false;
        boolean isSlept = false;
        boolean isRooted = false;

        Enemy(String enemyName, int health, int damage) {
            this.enemyName = enemyName;
            this.health = health;
            this.damage = damage;
            this.max_health = health;
        }

        void takeDamage(int amount) {
            health -= amount;
            if (health <= 0) {
                health = 0;
                isAlive = false;
            }
        }

        void setStunned(boolean stun) { isStunned = stun; }
        void setSleep(boolean sleep) { isSlept = sleep; }
        void setRooted(boolean rooted) { isRooted = rooted; }
        boolean isAlive() { return isAlive; }
        boolean isStunned() { return isStunned; }
        boolean isSlept() { return isSlept; }
        boolean isRooted() { return isRooted; }
        String getName() { return enemyName; }
    }

    static class Battle {
        Character[] party;
        Enemy[] enemies;
        private TextArea log;
        Random random = new Random();
        public String selectedSpell;
        boolean healed = false;
        int partyAttackOrDefend = 0; // 0 for default, 1 for Attack, 2 for Defend

        Enemy revengeTarget = null;
        boolean SHealed = false;
        boolean CRITICAL = false;

        Battle(Character[] party, Enemy[] enemies, TextArea log) {
            this.party = party;
            this.enemies = enemies;
            this.log = log;
        }

        private SceneNode protectMenu;
        private SceneNode battleNode;
        private SceneNode spellsMenu;
        private SceneNode targetMenu;
        private SceneNode strategyMenu;
        public void setMenus(SceneNode protectMenu, SceneNode spellsMenu, SceneNode targetMenu, SceneNode strategyMenu, SceneNode battleNode) {
            this.protectMenu = protectMenu;
            this.spellsMenu = spellsMenu;
            this.targetMenu = targetMenu;
            this.strategyMenu = strategyMenu;
            this.battleNode = battleNode;
        }
        public void refreshMenus() {
            protectMenu.setChoices(getProtectChoices(party[0], spellsMenu, battleNode));
            spellsMenu.setChoices(getSpellChoices(party[0], protectMenu, targetMenu, battleNode, battleNode));
            targetMenu.setChoices(getTargetingChoices(party[0], spellsMenu, battleNode));
            strategyMenu.setChoices(getStrategyChoices(battleNode, battleNode));
        }
        public void refreshProtectAndTargetMenus() {
            protectMenu.setChoices(getProtectChoices(party[0], spellsMenu, battleNode));
            targetMenu.setChoices(getTargetingChoices(party[0], spellsMenu, battleNode));
        }


        void characterAttack(Character c, Enemy enemy, int damage) {
            if (c.getName().equals("Eli") && c.isAttackUp()) { // only for Eli here as partyTurn handles the party
                damage = (int) (c.getWeaponDamage()+(c.getWeaponDamage()*0.5));
            }
            enemy.takeDamage(damage);
        }

        void characterHeal(Character caster) {
            Character lowest = null;
            for (Character c : party) {
                if (c.health < c.max_health) {
                    if (lowest == null || c.health < lowest.health) {
                        lowest = c;
                    }
                }
            }
            if (lowest != null && (lowest.max_health - lowest.health) > 0.20 * lowest.max_health) {
                // heal only if there is a party member with more than 20% hp missing
                int healAmount;
                if (caster.getName().equals("Ch")) { // Ch only heals for 2/3 of his weapon damage
                    healAmount = (int) (caster.getWeaponDamage() * 0.6);
                }
                else { // T and Sea heal for 2x their damage.
                    healAmount = caster.getWeaponDamage() * 2;
                }
                lowest.health += healAmount;
                if (lowest.health > lowest.max_health) {
                    lowest.health = lowest.max_health;
                }
                if (caster.getName().equals(lowest.getName())) { // if caster is lowest
                    log.appendText(caster.getName() + " heals himself for " + healAmount + "!\n");
                    if (caster.getName().equals("Sea")) {
                        log.appendText("Sea: \"If you want something done right...\"\n");  
                    }
                    else if (caster.getName().equals("T")) {
                        log.appendText("T: \"Don't worry about me, guys. I've got it!\"\n");
                    }
                    else if (caster.getName().equals("Ch")) {
                        log.appendText("Ch: \"Better than nothing.\"\n");
                    }
                }
                else { // if caster is healing someone else
                    log.appendText(caster.getName() + " heals " + lowest.getName() + " for " + healAmount + "!\n");
                    if (caster.getName().equals("Sea")) {
                        if (lowest.getName().equals("S") || lowest.getName().equals("Ch")) {
                            log.appendText("Sea: \"Always getting yourself into trouble, aren't you...\"\n");
                        }
                        else {
                            log.appendText("Sea: \"You're welcome.\"\n");
                        }
                    }
                    else if (caster.getName().equals("T")) {
                        if (lowest.getName().equals("Jim")) {
                            log.appendText("T: \"I've always got your back, Jim.\"\n");
                        }
                        else {
                            log.appendText("T: \"I've got you!\"\n");
                        }
                    }
                    else if (caster.getName().equals("Ch")) {
                        if (lowest.getName().equals("S")) {
                            log.appendText("Ch: \"You've gotta start being more careful.\"\n");
                        }
                        else {
                            log.appendText("Ch: \"Sorry! That's the best I can do.\"\n");
                        }
                    }
                }
                healed = true;
                if (lowest.isAlive == false) {
                    lowest.isAlive = true; // reset isAlive when brought back to life
                }
            }
        }

        int generateEnemyIdx() {
            int aliveCount = 0;
            for (Enemy e : enemies) {
                if (e.isAlive()) aliveCount++;
            }
            if (aliveCount == 0) return -1; // all dead, handle as needed
            int idx;
            do {
                idx = random.nextInt(enemies.length);
            } while (!enemies[idx].isAlive());

            return idx;
        }

        int generatePartyIdx() {
            int aliveCount = 0;
            for (Character c : party) {
                if (c.isAlive()) aliveCount++;
            }
            if (aliveCount == 0) return -1; // all party members are dead
            int idx;
            do {
                idx = random.nextInt(party.length);
            } while (!party[idx].isAlive());
            return idx;
        }


        void partyTurn() {
            for (Character c : party) {
                if (c.isAlive()) {
                    int coin = random.nextInt(2); // roll a 0 or 1
                    if (generateEnemyIdx() >= 0) { // only run party AI if a valid IDX can be created
                        int enemyIdx = generateEnemyIdx();
                        if (!isWon() && ((partyAttackOrDefend != 2) || c.isLockedInAttack())) { // if party commanded to NOT defend (attack/normal)

                            int damage = c.getWeaponDamage();
                                if(c.isAttackUp()) {
                                    damage = (int) (c.getWeaponDamage() + c.getWeaponDamage()*0.5);
                            }

                            if (c.getName().equals("Cl")) { // Cl
                                if (c.health < c.max_health*0.35 && partyAttackOrDefend == 0) { // Guard 50% of the time when low
                                    if (coin == 1) {
                                        c.setDefending(true);
                                        log.appendText(c.getName() + " braces for the next attacks!\n");
                                    }
                                }
                                else { // Cl's eldritch blast
                                    int enemyIdx2 = generateEnemyIdx();
                                    characterAttack(c, enemies[enemyIdx], damage);
                                    characterAttack(c, enemies[enemyIdx2], damage);
                                    if (enemies[enemyIdx] == enemies[enemyIdx2] || enemyIdx == enemyIdx2) {
                                        log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " twice for " + damage*2 + " total damage!\n");
                                    }
                                    else {
                                        log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " and " + enemies[enemyIdx2].getName() + " for " +
                                        damage + " damage each!\n");
                                    }
                                }
                            }

                            else if (c.getName().equals("Jim")) { // Jim does not defend unless instructed
                                int coin3 = random.nextInt(3);
                                if (coin3 == 0) { // 1/3 chance to double stab
                                    characterAttack(c, enemies[enemyIdx], damage*2);
                                    log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " twice for " + damage*2 + " total damage!\n");
                                }
                                else {
                                    characterAttack(c, enemies[enemyIdx], damage);
                                    log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " for " + damage + "!\n");
                                }
                            }

                            else if (c.getName().equals("M")) {
                                int cd = c.getCooldown("One-shot Kill");
                                if (partyAttackOrDefend == 2) {
                                    log.appendText(c.getName() + " is too focused to defend now.\n");
                                }

                                if (coin == 1 && c.health < c.max_health*0.3 && !c.isLockedInAttack() && partyAttackOrDefend == 0) { // coin decide to defend when low
                                    c.setDefending(true);
                                    log.appendText(c.getName() + " braces for the next attacks!\n");
                                }
                                else if (cd == 0) { // first turn set up
                                    c.setCooldown("One-shot Kill", 3);
                                    log.appendText("M prepares to take a shot...\n");
                                    c.setLockedInAttack(true);
                                }
                                else if (cd == 2) { // 2nd turn setup
                                    log.appendText("M's got her eye trained on her target...\n");
                                }
                                else if (cd == 1) { // fire!!!!
                                    Enemy target = null;
                                    for (Enemy e : enemies) {
                                        if (target == null || (e.health > target.health && e.isAlive())) {
                                            target = e;
                                        }
                                    }
                                    characterAttack(c, target, (int) (damage*4));
                                    log.appendText("M takes a shot! " + target.getName() + " is hit for " + (int) (damage*4) + " damage!\n");
                                    c.setLockedInAttack(false);
                                }
                            }

                            else if (c.getName().equals("D")) { // D
                                if (c.health < c.max_health*0.20 && coin == 1 && partyAttackOrDefend == 0) { // 50% chance to defend at less than 20% hp
                                    c.setDefending(true);
                                    log.appendText(c.getName() + " braces for the next attacks!\n");
                                }
                                else {
                                    log.appendText(c.getName() + " attacks ");
                                    int aliveCount = 0;
                                    for (Enemy eCount : enemies) if (eCount.isAlive()) aliveCount++;
                                    for (int i = 0; i < enemies.length; i++) { // D damage scales with more HP%
                                        // Damage updates here to be -2 or +2 based on on enemy HP
                                        // 1 per 25%, so flat damage at 50%, penalty at -50%
                                        if (enemies[i].isAlive()) {
                                            if (i+1 == enemies.length && aliveCount > 1) { // don't trigger when only 1 enemy alive
                                                log.appendText("and ");
                                            }
                                            double hpPercent = (double) enemies[i].health / enemies[i].max_health * 100;
                                            if (hpPercent >= 75) {
                                                damage = c.getWeaponDamage() + 2;
                                            } else if (hpPercent >= 50) {
                                                damage = c.getWeaponDamage() + 1;
                                            } else if (hpPercent >= 25) {
                                                damage = c.getWeaponDamage() - 1;
                                            } else if (hpPercent < 25) {
                                                damage = c.getWeaponDamage() - 2;
                                            }
                                            if (c.isAttackUp()) {
                                                damage = (int) (damage + damage*0.5);
                                            }
                                            characterAttack(c, enemies[i], damage);
                                            log.appendText(enemies[i].getName() + " for " + damage);
                                            if (i+1 == enemies.length) { // as long as not last enemy and more than one alive
                                                log.appendText("!\n");
                                            }
                                            else {
                                                log.appendText(", ");
                                            }
                                        }
                                    }
                                }
                            }

                            else if (c.getName().equals("Ch")) {
                                if (c.canUse("Tiny Heal")) { // Ch will heal alongside his normal turn.
                                    characterHeal(c);
                                    c.setCooldown("Tiny Heal", 2);
                                    healed = false; // Ch isn't even supposed to use this but it auto triggers
                                }
                                if (coin == 1 && c.health < c.max_health*0.25 && partyAttackOrDefend == 0) {
                                    c.setDefending(true);
                                }
                                else if (c.canUse("Entangle")) { // Ch prioritizes his AOE root after defending
                                    for (Enemy e : enemies) {
                                        e.setRooted(true);
                                        characterAttack(c, e, damage);
                                    }
                                    log.appendText(c.getName() + " roots all enemies to the ground and deals " + damage + " damage!\n");
                                    c.setCooldown("Entangle", 3);
                                }
                                else {
                                    characterAttack(c, enemies[enemyIdx], damage);
                                    log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " for " + damage + "!\n");
                                }
                            }

                            else if (c.getName().equals("T")) { // Tobes
                                if (partyAttackOrDefend == 0) {
                                    characterHeal(c); // T prioritizes healing
                                }
                                int enemyIdx2 = generateEnemyIdx();
                                if (!healed) {
                                    if (c.health < c.max_health*0.25 && coin == 1 && partyAttackOrDefend == 0) { // 50% chance to defend at less than 25% hp
                                        c.setDefending(true);
                                        log.appendText(c.getName() + " braces for the next attacks!\n");
                                    }
                                    else if (c.canUse("Lullaby") && partyAttackOrDefend == 0) { // T will sleep an enemy
                                        if (enemies[enemyIdx].isStunned() && enemies[enemyIdx].isRooted()) { // if the enemy is already stunned/rooted
                                            if (!enemies[enemyIdx2].isStunned() && !enemies[enemyIdx2].isRooted()) { // attempt to target another one
                                                enemies[enemyIdx2].setSleep(true);
                                                log.appendText(c.getName() + " plays a soothing lullaby! " + enemies[enemyIdx2].getName() + " falls asleep!\n");
                                                c.setCooldown("Lullaby", 2); // every other turn
                                            }
                                        } // T will only sleep if one of the 2 enemies is targetable for sleep.
                                        else { // enemy is not already stunned or rooted
                                            enemies[enemyIdx].setSleep(true);
                                            log.appendText(c.getName() + " plays a soothing lullaby! " + enemies[enemyIdx].getName() + " falls asleep!\n");
                                            c.setCooldown("Lullaby", 2); // every other turn
                                        }
                                    }  
                                    else { // Attack
                                        characterAttack(c, enemies[enemyIdx], damage);
                                        log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " for " + damage + "!\n");
                                    }
                                }
                                healed = false;
                            }

                            else if (c.getName().equals("S")) {
                                // S critically strikes if he's protected or if he's taken damage last turn
                                // if S is enraged he will ignore defend orders
                                if (partyAttackOrDefend == 2 && (c.isLockedInAttack() || CRITICAL)) {
                                    log.appendText(c.getName() + " refuses to defend!\n");
                                }
                                if (c.isLockedInAttack() || CRITICAL) {
                                    if (revengeTarget != null) {
                                        characterAttack(c, revengeTarget, (int) (damage*2.5));
                                        log.appendText(c.getName() + " attacks " + revengeTarget.getName() + " savagely for " + (int) (damage*2.5) + "!\n");
                                    }
                                    else {
                                        characterAttack(c, enemies[enemyIdx], (int) (damage*2.5));
                                        log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " savagely for " + (int) (damage*2.5) + "!\n");
                                    }
                                    c.setLockedInAttack(false);
                                    CRITICAL = false;
                                }
                                else if (!SHealed && c.health < c.max_health*0.4) {
                                    c.health += (int) (c.max_health*0.3);
                                    SHealed = true;
                                    log.appendText(c.getName() + " heals himself for " + (int) (c.max_health*0.3) + "!\n");
                                }
                                else {
                                    characterAttack(c, enemies[enemyIdx], damage);
                                    log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " for " + damage + "!\n");
                                }
                            }

                            else if (c.getName().equals("Sea")) { // Sea
                                if (partyAttackOrDefend == 0) {
                                    characterHeal(c); // Sea prioritizes healing
                                }
                                if (!healed) {
                                    // If no one was healed this turn
                                    if (partyAttackOrDefend == 1 || (!c.canUse("Attack Buff") && c.health >= c.max_health*0.35)) {
                                        characterAttack(c, enemies[enemyIdx], damage);
                                        log.appendText(c.getName() + " attacks " + enemies[enemyIdx].getName() + " for " + damage + "!\n");
                                    }
                                    else if (c.health < c.max_health*0.35 && !c.canUse("Attack Buff")) { // Guards when low & can't buff
                                        c.setDefending(true);
                                        log.appendText(c.getName() + " braces for the next attacks!\n");
                                    }
                                    if (c.isAttackUp()) {
                                        c.setAttackUp(false);
                                        log.appendText("Sea' attack buff has worn off.\n");
                                    }
                                    if (c.canUse("Attack Buff")) {
                                        for (Character target : party) {
                                            target.setAttackUp(true);
                                        }
                                        log.appendText("Sea buffs each party member's next attack by 50%!\n");
                                        c.setCooldown("Attack Buff", 2);
                                    }
                                }
                                else if (c.isAttackUp()) {
                                    c.setAttackUp(false);
                                    log.appendText("Sea' attack buff has worn off.\n"); // only prompt the message when it cycles back to Sea' turn
                                }
                                healed = false; // reset healed for other healers
                            }
                            if (!c.getName().equals("Sea")) {
                                c.setAttackUp(false); // reset attackUp for all except Sea here
                            }
                        }
                        else if (partyAttackOrDefend == 2) { // if party commanded to defend
                            if (!c.getName().equals("Eli")) {
                                c.setDefending(true); 
                                c.setAttackUp(false); // reset attackUp here too
                            }
                        }
                    }
                }
            }

            if (this.isWon()) {
                log.appendText("You won!\n");
            }
            else {
                this.enemyAttack();
            }
        }

        void enemyAttack() {
            int damage;
            Character character;

            boolean wasRootedThisTurn = false;
            if (enemies[0].isRooted()) {
                wasRootedThisTurn = true;
                log.appendText("All enemies are rooted...\n");
            }
            
            for (Enemy e : enemies) {
                if (e.isAlive() && generatePartyIdx() >= 0) {
                    damage = e.damage;
                    character = party[generatePartyIdx()];

                    if (e.isRooted()) {
                        damage = 0;
                    }
                    else if (e.isStunned()) {
                        damage = 0;
                        log.appendText(e.enemyName + " is stunned!\n");
                    }
                    else if (e.isSlept()) {
                        damage = 0;
                    }
                    else if (character.getName().equals("Jim")) { // Jim has a 20% dodge rate
                        int prob = random.nextInt(20);
                        if (prob <= 3) { // 4/20 chance
                            damage = 0;
                            log.appendText("Jim dodges " + e.getName() + "'s attack!\n");
                        }
                        else {
                            log.appendText(e.enemyName + " attacks " + character.getName() + " for " + damage + " damage!\n");
                        }
                    }
                    else if (character.isDefending()) {
                        damage = (damage + 1) / 2; // halve damage
                        log.appendText(e.enemyName + " attacks! " + character.getName() +
                            " braces and takes only " + damage + " damage!\n");
                    }
                    else if (character.isProtected()) {
                        damage = 0;
                        log.appendText(e.enemyName + " attacks! " + character.getName() +
                        " is protected and takes " + damage + " damage!\n");
                    }
                    else {
                        log.appendText(e.enemyName + " attacks " + character.getName() + " for " + damage + " damage!\n");
                    }
                    character.takeDamage(damage);
                    if (!character.isAlive()) {
                        log.appendText(character.getName() + " collapsed!\n");
                    }
                    if (character.getName().equals("S") && damage > 0 && character.isAlive()) { // if S was struck this turn, trigger rage
                        revengeTarget = e;
                        if (character.isLockedInAttack()) {
                            log.appendText("S: \"Seriously?\"\n");
                        }
                        else {
                            log.appendText("S: \"You'll pay for that!\"\n");
                        }
                        character.setLockedInAttack(true);
                    }

                    e.setStunned(false); // reset stun
                    e.setRooted(false); // reset root
                    if (e.isSlept()) {
                        e.setSleep(false);
                        log.appendText(e.getName() + " woke up!\n");
                    }
                }
            }
            if (wasRootedThisTurn) { // if any enemy was rooted
                log.appendText("All enemies break free from the roots!\n");
                wasRootedThisTurn = false;
            }
            for (Character c : party) { // reset values & reduce cooldowns
                c.setProtected(false);
                c.setDefending(false);
                c.reduceCooldowns();
            }
            
            if (isLost()) {
                log.appendText("All party members have fallen...\n");
            }
            else if (partyAttackOrDefend != 0 && !isWon()) {
                partyAttackOrDefend = 0;
                log.appendText("Party behavior reset!\n");
            }

            log.selectPositionCaret(log.getLength());
            log.deselect();

            refreshProtectAndTargetMenus(); // refresh all menus at the end of the turn
        }

        void playerDefend() {
            party[0].setDefending(true);
            log.appendText("\n" + party[0].getName() + " braces for the next attacks!\n");
            this.partyTurn();
        }

        public SceneNode.Choice[] getSpellChoices(Character caster, SceneNode protectMenu, SceneNode targetingMenu, SceneNode backNode, SceneNode afterActionNode) {
            SceneNode.Choice[] choices = new SceneNode.Choice[4];
            choices[0] = new SceneNode.Choice(
                "Spark", 
                targetingMenu, 
                () -> !isWon(), 
                () -> {
                    selectedSpell = "Spark";
                    
                }, 
                "Electrocute your foes for medium damage."
            );
            choices[1] = new SceneNode.Choice(
                "Protect",
                protectMenu,
                () -> !isWon() && caster.canUse("Protect"),
                () -> {
                },
                caster.canUse("Protect") ? "Protect a chosen party member from all incoming damage. (Cooldown: 2 turns)" : "On Cooldown"
            );
            choices[2] = new SceneNode.Choice(
                "Static Shock",
                targetingMenu,
                () -> !isWon() && caster.canUse("Static Shock"),
                () -> {
                    selectedSpell = "Static Shock";
                },
                caster.canUse("Static Shock") ? "Stun an enemy for a turn and deal minor damage. (Cooldown: 1 turn)" : "On Cooldown"
            );
            choices[3] = new SceneNode.Choice("Back", afterActionNode, () -> true, null, null);
            return choices;
        }

        public SceneNode.Choice[] getTargetingChoices(Character caster, SceneNode backNode, SceneNode afterActionNode) {
            List<SceneNode.Choice> choices = new ArrayList<>();

            for (Enemy target : enemies) {
                if (target.isAlive()) {
                    choices.add(new SceneNode.Choice(target.getName(), afterActionNode, () -> !isWon(), () -> {
                        if (selectedSpell.equals("Spark")) {
                            log.appendText("\n" + caster.getName() + " casts Spark! ");
                            this.characterAttack(caster, target, caster.getWeaponDamage());
                            if (party[0].isAttackUp()) {
                                log.appendText(target.getName() + " takes " + (int) (caster.getWeaponDamage() * 1.5) + " damage!\n");
                            } else {
                                log.appendText(target.getName() + " takes " + caster.getWeaponDamage() + " damage!\n");
                            }
                            this.partyTurn();
                        }
                        else if (selectedSpell.equals("Static Shock")) {
                            caster.setCooldown("Static Shock", 2); // actually 1
                            log.appendText("\n" + caster.getName() + " casts Static Shock on " + target.enemyName + "! ");
                            target.setStunned(true);
                            this.characterAttack(caster, target, caster.getWeaponDamage() / 3);
                            if (party[0].isAttackUp()) {
                                log.appendText(target.getName() + " takes " + (int) (caster.getWeaponDamage() / 3 * 1.5) + " damage!\n");
                            } else {
                                log.appendText(target.getName() + " takes " + caster.getWeaponDamage() / 3 + " damage!\n");
                            }
                            this.partyTurn();
                        }
                    }, null));
                }
            }
            choices.add(new SceneNode.Choice("Back", backNode, () -> true, null, null));
            return choices.toArray(new SceneNode.Choice[0]);
        }

        public SceneNode.Choice[] getProtectChoices(Character caster, SceneNode backNode, SceneNode afterActionNode) {
            List<SceneNode.Choice> choices = new ArrayList<>();

            for (Character target : party) {
                if (target.isAlive()) {
                    choices.add(new SceneNode.Choice(target.getName(), afterActionNode, () -> !isWon(), () -> {
                        caster.setCooldown("Protect", 3); // actually 2
                        if (target == caster) {
                            log.appendText("\n" + caster.getName() + " casts Protect on himself!\n");
                        } else {
                            log.appendText("\n" + caster.getName() + " casts Protect on " + target.getName() + "!\n");
                            if (target.getName().equals("Cl")) { // Character comment
                                log.appendText("Cl: \"Thanks!\"\n");
                            }
                            else if (target.getName().equals("Jim")) {
                                log.appendText("Jim: \"Appriciate it!\"\n");
                            }
                            else if (target.getName().equals("Sea")) {
                                log.appendText("Sea: \"Smart.\"\n");
                            }
                            else if (target.getName().equals("T")) {
                                log.appendText("T: \"Phew. Thanks!\"\n");
                            }
                            else if (target.getName().equals("M")) {
                                log.appendText("M: \"Good choice.\"\n");
                            }
                            else if (target.getName().equals("D")) {
                                log.appendText("D: \"Thank you!\"\n");
                            }
                            else if (target.getName().equals("Ch")) {
                                log.appendText("Ch: \"Thanks, man!\"\n");
                            }
                            else if (target.getName().equals("S")) {
                                log.appendText("S: \"Let me at 'em!\"\n");
                                CRITICAL = true;
                            }
                        }
                        target.setProtected(true);
                        partyTurn();
                    }, "Protect a chosen party member from all damage. (Cooldown: 1 turn)"));
                }
            }
            choices.add(new SceneNode.Choice("Back", backNode, () -> true, null, null));
            return choices.toArray(new SceneNode.Choice[0]);
        }

        public SceneNode.Choice[] getStrategyChoices(SceneNode backNode, SceneNode afterActionNode) {
            SceneNode.Choice[] choices = new SceneNode.Choice[4];

            choices[0] = new SceneNode.Choice("Attack", afterActionNode, () -> !isWon(), () -> {
                partyAttackOrDefend = 1;
                log.appendText("\nYour party will attack this turn.\n");
            }, "Tell your party members to attack this turn.");
            choices[1] = new SceneNode.Choice("Defend", afterActionNode, () -> !isWon(), () -> {
                partyAttackOrDefend = 2;
                log.appendText("\nYour party will defend this turn.\n");
            }, "Tell your party members to defend this turn.");
            choices[2] = new SceneNode.Choice("Reset", afterActionNode, () -> !isWon(), () -> {
                partyAttackOrDefend = 0;
                log.appendText("\nYour party will decide on their own.\n");
            }, null);
            choices[3] = new SceneNode.Choice("Back", backNode, () -> true, null, null);

            return choices;
        }


        boolean isWon() {
            Boolean win = true;
            for (Enemy enemy : enemies) {
                if (enemy.isAlive()) {
                    win = false;
                    break;
                }
            }
            return win;
        }

        boolean isLost() { 
            Boolean lost = true;
            for (Character character : party) {
                if (character.isAlive()) {
                    lost = false;
                    break;
                }
            }
            return lost;
        }

        void healAll() {
            // Reset values again just in case
            for (Character c : party) {
                c.health = c.max_health;
                c.isAlive = true;
                c.resetCooldowns();
                c.setProtected(false);
                c.setDefending(false);
            }
            SHealed = false;
            revengeTarget = null;
            CRITICAL = false;
        }

        void resetEnemyHP() {
            for (Enemy e : enemies) {
                e.health = e.max_health;
                e.setStunned(false);
                e.setRooted(false);
                e.setSleep(false);
            }
        }

        String displayEnemies() {
            StringBuilder sb = new StringBuilder();
            for (Enemy e : enemies) {
                sb.append(e.enemyName).append(" HP: ").append(e.health).append("/").append(e.max_health).append("   ");
            }
            sb.append("\n\n");
            return sb.toString();

        }

        String displayParty() {
            StringBuilder sb = new StringBuilder();
            for (Character c : party) {
                sb.append(c.getName()).append(" HP: ").append(c.health).append("/").append(c.max_health).append("   ");
            }
            return sb.toString();
        }
    }

    private TextFlow storyText = new TextFlow();
    private TextArea battleLog = new TextArea();
    private Button leftButton = new Button();
    private Button centerButton = new Button();
    private Button rightButton = new Button();
    private Button fourthButton = new Button();
    private Button fifthButton = new Button();
    private Button sixthButton = new Button(); // goddamn theres so many of these now


    @Override
    public void start(Stage stage) {

        // Create Characters
        Character Eli = new Character("Eli", "Wizard", 17, "Staff", 6,
            "A curious newcomer intent on discovering the truth.");
        Character Cl = new Character("Cl", "Warlock", 21, "Glaive", 6,
            "The sorcerer king's mysterious stooge.");
        Character J = new Character("J", "Paladin", 26, "Broadsword", 7, 
            "A loyal knight with a heart of gold and a will of steel.");
        Character D = new Character("D", "Sorcerer", 21, "None", 5, 
            "A good-hearted waitress with fiery magic.");
        Character M = new Character("M", "Ranger", 18, "Bow", 10, 
            "A sharp-witted deadeye who specializes in tracking and hunting down prey.");
        Character T = new Character("T", "Bard", 18, "Lute", 5, 
            "Jim's best friend. A cheerful performer with a knack for storytelling.");
        Character Sea = new Character("Sea", "Cleric", 20, "Tome", 4,
            "An austere potionmaker who values his own agenda on par with that of his Goddess'.");
        Character Ch = new Character("Ch", "Druid", 25, "Twin Machetes", 7, 
            "A friendly hermit who lives near the dragon's den.");
        Character S = new Character("S", "Barbarian", 23, "Greataxe", 9, 
            "The fearsome dragon's human form.");

        Character[] party = {Eli, S, J, D};
        // add characters to not full party with addToParty(character, idx)
        ArrayList<Character> knownPartyMembers = new ArrayList<>();
            knownPartyMembers.add(Eli);
            knownPartyMembers.add(Cl);
            knownPartyMembers.add(J);
            knownPartyMembers.add(D);
            knownPartyMembers.add(M);
            knownPartyMembers.add(T);
            knownPartyMembers.add(Sea);
            knownPartyMembers.add(Ch);
            knownPartyMembers.add(S);
        
        theFacts knowledge = new theFacts(party);
        
        for (Character c : knownPartyMembers) {
            knowledge.addToPartyList(c);
        }

        // Create SceneNodes (input Text, or Text, Runnable)
        SceneNode spellsMenu = new SceneNode("Choose a spell:");
        SceneNode protectMenu = new SceneNode("Choose a target to protect:");
        SceneNode targetMenu = new SceneNode("Choose a target:");
        SceneNode strategyMenu = new SceneNode("Tell your teammates what to do:");
        SceneNode partySwapOutMenu = new SceneNode("Which party member do you want to swap out?");
        SceneNode partySwapInMenu = new SceneNode("Which party member do you want to swap in?");
        SceneNode viewParty = new SceneNode(knowledge.partyInfo());

        // TODO : actually script story
        SceneNode start = new SceneNode(makeTextFlow(
            "Welcome to the game! This is the debugging menu. ",
                italicize("Press continue to play.")));
        SceneNode enterSetting = new SceneNode(makeTextFlow(
            "You're standing in a huge hallway outside the massive doors that lead to the throne room. " +
            "You've been standing there in complete silence for at least twenty minutes. Long enough that " +
            "you've started to count the brass studs inlaid upon the wooden surface of the doors.\n\n" + 
            "You didn't mind waiting, but it would've been nice to have a little more information to play " +
            "with while you did. Nobody has told you anything. Not since you got here. You'd taken the job " + 
            "request despite how vague it was, traveled days to a city you'd never been to, approached the " + 
            "guards at the front gate and been promptly ushered in, told to wait just a moment… and then " + 
            "nothing. A whole lot of hurry for a whole lot of nothing.\n\nAll the request had said was: ",
            italicize("5,000 gold pieces to fetch something important from somewhere dangerous. TOWN NAME. Report to NAME Castle. More information will be disclosed on site.\n\n\n(Click to continue.)")
        ));
        SceneNode enterSetting2 = new SceneNode(makeTextFlow(
            "You're beginning to think this was a bad idea. There was a reason nobody else took it. But you " + 
            "couldn't help your curiosity. Something important? Can't be ", italicize("that"),
            " important if they were only offering 5,000 gold pieces. That was only a little over three months rent where you lived. And " + 
            "how dangerous? 'Acid swamp diving' dangerous? 'Ancient sorcerer tomb robbing' dangerous? " + 
            "Or the kind of dangerous that only a pompous prince who's never seen battle would find dangerous?\n\n" + 
            "You tug at the brim of your wizard's hat with a little sigh. You suppose it didn't matter too " + 
            "much. Student loans don't disappear with a spell, and your prestigious magic school wasn't " + 
            "cheap… You'd do just about anything to make a dent in them, as long as it wasn't straight up stupid."
        ));
        SceneNode approachKing = new SceneNode(
            "With a creak and groan that surprises you, the throne room doors are pushed open from the " +
            "inside by two guards, who bow to you and hold them open to let you pass through. You step " +
            "past them tentatively. It feels weird to be bowed to. You are nobody special.\n\n" +
            "Seated upon the throne is a large man with a heavy red cape and bejeweled bracers. He had a " +
            "clean-shaved face and sallow skin and thin, stringy brown hair. There was a young woman " +
            "standing off to his left with a glaive in her right hand. Her neat black hair was clipped to keep it " +
            "away from her face. They both watched your approach silently.\n\n"
        );
        SceneNode introduceKing = new SceneNode(
            "Fifty feet from the foot of his throne, the king put up a hand to stop you.\n\n" +
            "“Hello,” you say awkwardly from where you stand. At first, he did not say anything. You " +
            "wonder if he was expecting you to bow or kneel. You certainly could have. Maybe that " +
            "would've been better. Given you were a stranger from afar and here purely for the job, you just " +
            "hadn't seen a reason to.\n\n" +
            "The king finally spoke. “A pleasure to meet you,” he said. His voice was deep and smooth. “What should I call you?”\n\n" +
            "The question puzzles you with its vagueness but only for a moment. “Eli,” you say earnestly. " +
            "“My name is Eli.”", 
        () -> {
            knowledge.metKing = true;
        });
        SceneNode introduceQuest1 = new SceneNode(
            "“Tell me, wizard Eli, what do you have to offer me?”\n\n" +
            "Again, a confusing question. You disliked pointless screening and unclear questions.\n\n" +
            "“I'm a recent graduate from Ghicellor. If you'd tell me what the request is for, I can tell you " +
            "exactly how I'd go about fulfilling it for you. As it stands, I don't know enough to say if I'm a good fit.”"
        );
        SceneNode introduceQuest2 = new SceneNode(makeTextFlow(
            "The king smiled slightly.\n\n" +
            "“There is a young dragon to the west that has been terrorizing the city for quite some time " +
            "now. We've been trying to deal with it on our own but our efforts have been mostly in vain… for now.”\n\n" +
            "As he continued, his voice grew thick with contempt.\n\n" +
            "“You know how dragons are… selfish, narcissistic, ", italicize("greedy…"),
            " insatiable hoarders! The whelp has stolen something important from me. You are to steal it back.”"
        ));
        SceneNode introduceJim = new SceneNode(
            "A barrage of questions hit your mind all at once. “Just the one thing?” you ask.\n\n" +
            "“Yes. I can stand to lose the rest, but this… it must be returned. No matter the cost.”\n\n" +
            "Cost? Did the king expect him to die for 5,000 gold pieces? Who on earth would agree to that?\n\n" +
            "“I'd like to introduce you to someone,” the king said. “If you agree to help us, you'll be working with him.”\n\n" +
            "One of the guards that'd opened the door for you stepped forward and marched towards you, " +
            "his armor clanking with each step he took. He had to be around your age. His blue eyes agleam " +
            "with purpose, he shook your hand with vigor.\n\n" +
            "“J. Pleased to make your acquaintance!”\n\n" +
            "You nod in acknowledgment.\n\n" + 
            "“Eli.”\n\n"        
        );




        SceneNode introduceCl = new SceneNode("Text Cl.", 
        () -> {
            knowledge.metCl = true;
        });

        /**
         * BATTLE_1 START
         */
        Enemy slime = new Enemy("Slime", 70, 10);
        Enemy goobert = new Enemy("Goobert", 85, 15);
        Enemy[] enemies = {slime, goobert};
        Battle slimebattle = new Battle(knowledge.getParty(), enemies, battleLog);

        SceneNode battleTest = new SceneNode("", () -> {
            storyText.getChildren().clear();
            
            storyText.getChildren().add(makeTextFlow(
                "This is a battle test scene.\n\n" +
                slimebattle.displayEnemies() +
                slimebattle.displayParty()
            ));
            battleLog.setVisible(true);
            if (battleLog.getText().isEmpty()) {
                battleLog.appendText("Battle begins!\n");
            }
        });
        /**
         * BATTLE_1 END
         */

        SceneNode postBattle = new SceneNode("The slime lies defeated.", () -> {
            battleLog.setVisible(false);
            battleLog.clear();
        });
        SceneNode end = new SceneNode("This is the final scene.");

        // Connect SceneNodes via nextOnClick
        postBattle.nextOnClick = end;
        enterSetting.nextOnClick = enterSetting2;
        enterSetting2.nextOnClick = approachKing;
        approachKing.nextOnClick = introduceKing;
        introduceKing.nextOnClick = introduceQuest1;
        introduceQuest1.nextOnClick = introduceQuest2;
        introduceQuest2.nextOnClick = introduceJim;
        introduceJim.nextOnClick = introduceCl;

        // Connect SceneNodes via Choices
        start.setChoices(
            new SceneNode.Choice("Continue", enterSetting),
            new SceneNode.Choice("Battle (For Testing)", battleTest),
            new SceneNode.Choice("View Party (For Testing)", viewParty)
        );
        knowledge.linkMenus(viewParty, partySwapOutMenu, partySwapInMenu, start); // start is 'back'

        /** 
         * BATTLE_1 CHOICES START
         */
        slimebattle.setMenus(protectMenu, spellsMenu, targetMenu, strategyMenu, battleTest);
        knowledge.setOnPartyChange(() -> {
            slimebattle.refreshMenus();
            knowledge.refreshViewParty(start); // param is 'back'
        });
        slimebattle.refreshMenus(); // bc the reference broke if you dont change the party
        knowledge.refreshViewParty(start);

        battleTest.setChoices(
            new SceneNode.Choice("Spells", spellsMenu, () -> !slimebattle.isWon() && !slimebattle.isLost() && slimebattle.party[0].isAlive()),
            new SceneNode.Choice("Defend", battleTest, () -> !slimebattle.isWon() && !slimebattle.isLost() && slimebattle.party[0].isAlive(), () -> {
                slimebattle.playerDefend();
            }, null),
            new SceneNode.Choice("Strategize", strategyMenu, () -> !slimebattle.isWon() && !slimebattle.isLost() && slimebattle.party[0].isAlive()),
            new SceneNode.Choice("Retry", battleTest, () -> slimebattle.isLost() && !slimebattle.party[0].isAlive(), () -> {
                slimebattle.healAll();
                slimebattle.resetEnemyHP();
                battleLog.clear();
                battleLog.appendText("Retrying the battle...\n");
            }, null),
            new SceneNode.Choice("Next Turn", battleTest, () ->  !slimebattle.isWon() && !slimebattle.party[0].isAlive() && !slimebattle.isLost(), () -> {
                battleLog.appendText("\n");                
                slimebattle.partyTurn(); // attempting fix for continuing battle if youre dead
            }, null),
            new SceneNode.Choice("Continue", postBattle, () -> slimebattle.isWon(), () -> {
                slimebattle.healAll();
            }, null)
        );
        /** 
         * BATTLE_1 CHOICES END
         */

        // UI Layout setup
        battleLog.setEditable(false);
        battleLog.setWrapText(true);
        battleLog.setPrefHeight(150);
        battleLog.setVisible(false);
        battleLog.setStyle("-fx-control-inner-background: #444444; -fx-text-fill: white; -fx-font-family: 'Georgia'; -fx-font-size: 14px;");

        layout = new VBox(15, storyText, leftButton, centerButton, rightButton, fourthButton, fifthButton, sixthButton, battleLog);
        layout.setStyle("-fx-padding: 20; -fx-font-size: 14; -fx-background-color: #1e1e1e;");
        storyText.setStyle("-fx-fill: #f0f0f0;");
        leftButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");
        centerButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");
        rightButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");
        fourthButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");
        fifthButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");
        sixthButton.setStyle("-fx-font-family: 'Georgia'; -fx-background-color: #333333; -fx-text-fill: #ffffff;");

        Scene scene = new Scene(layout, 700, 600);
        stage.setScene(scene);
        stage.setTitle("Grand Title");
        stage.show();

        showScene(start); // Start the story
    }

    void showScene(SceneNode node) {
        storyText.getChildren().clear();

        if (node != null && node.text != null) {
            for (javafx.scene.Node child : node.text.getChildren()) {
                if (child instanceof Text) {
                    Text cloned = copyTextNode((Text) child);
                    storyText.getChildren().add(cloned);
                } else {
                    storyText.getChildren().add(child);
                }
            }
        }

        if (node.onShow != null) {
            javafx.application.Platform.runLater(node.onShow);
        }

        Button[] buttons = { leftButton, centerButton, rightButton, fourthButton, fifthButton, sixthButton };
        for (Button btn : buttons) {
            btn.setVisible(false);
            btn.setOnAction(null);
        }
        layout.setOnMouseClicked(null);

        if (node.choices != null && node.choices.length > 0) {
            for (int i = 0; i < node.choices.length && i < buttons.length; i++) {
                SceneNode.Choice c = node.choices[i];
                Button b = buttons[i];

                boolean visible = (c.condition == null) || c.condition.get();
                if (visible) {
                    b.setText(c.text);
                    b.setVisible(true);

                    if (c.tooltip != null && !c.tooltip.isEmpty()) {
                        Tooltip tip = new Tooltip(c.tooltip);
                        b.setTooltip(tip);
                    } else {
                        b.setTooltip(null);
                    }
                    b.setOnAction(e -> {
                        if (c.action != null) c.action.run();
                        if (c.nextNode != null) showScene(c.nextNode);
                    });
                }
                else {
                    b.setVisible(false);
                }
            }
        }
        else if (node.nextOnClick != null) {
            layout.setOnMouseClicked(e -> showScene(node.nextOnClick));
        }
    }

    // --- Text Formatting ---
    private Text italicize(String text) {
        Text t = new Text(text);
        t.setStyle("-fx-font-family: 'Georgia'; -fx-font-size: 14; -fx-font-style: italic; -fx-fill: #f0f0f0;");
        return t;
    }

    private Text bold(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        t.setStyle("-fx-fill: #f0f0f0;");
        return t;
    }

    private Text title(String text) {
        Text t = new Text(text);
        t.setFont(Font.font("Georgia", FontWeight.BOLD, 20));
        t.setStyle("-fx-fill: #f0f0f0;");
        return t;
    }

    private static TextFlow makeTextFlow(Object... parts) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(4);
        for (Object p : parts) {
            Text t;
            if (p instanceof Text) {
                t = (Text) p;
            } else {
                t = new Text(String.valueOf(p));
                t.setFont(Font.font("Georgia", FontWeight.NORMAL, 14));
                t.setStyle("-fx-fill: #f0f0f0;");
            }
            flow.getChildren().add(t);
        }
        return flow;
    }

    private Text copyTextNode(Text src) {
        Text t = new Text(src.getText());
        // copy font if present
        Font f = src.getFont();
        if (f != null) t.setFont(Font.font(f.getFamily(), f.getStyle().contains("Bold") ? FontWeight.BOLD : FontWeight.NORMAL, f.getSize()));
        // copy style string (color etc.)
        t.setStyle(src.getStyle());
        return t;
    }

    private void appendToTextFlow(TextFlow flow, Object... parts) {
        TextFlow addition = makeTextFlow(parts);
        flow.getChildren().addAll(addition.getChildren());
    }   

    public static void main(String[] args) {
        launch();
    }
}